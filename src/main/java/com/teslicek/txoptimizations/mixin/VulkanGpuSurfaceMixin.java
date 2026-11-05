package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.device.GpuSurface;
import com.mojang.renderpearl.api.device.SurfaceException;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.backend.api.CommandEncoderBackend;
import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuSurface;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuTexture;
import com.mojang.renderpearl.backend.vulkan.VulkanQueue;
import com.mojang.renderpearl.backend.vulkan.VulkanUtils;
import com.teslicek.txoptimizations.AcquiredImageDiscard;
import com.teslicek.txoptimizations.PresentThread;
import com.teslicek.txoptimizations.gpu.GpuPassProfiler;
import it.unimi.dsi.fastutil.longs.LongList;
import java.nio.LongBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.KHRSynchronization2;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDependencyInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkImageBlit;
import org.lwjgl.vulkan.VkImageMemoryBarrier2;
import org.lwjgl.vulkan.VkImageSubresourceLayers;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkMemoryBarrier2;
import org.lwjgl.vulkan.VkOffset3D;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VulkanGpuSurface.class)
public abstract class VulkanGpuSurfaceMixin implements AcquiredImageDiscard {

    @Unique
    private static final int RESET_COMMAND_BUFFER = 2;

    @Unique
    private static final int SIMULTANEOUS_USE = 4;

    @Unique
    private static final long TRANSFER_STAGE = 4096L;

    @Unique
    private static final long TRANSFER_WRITE = 4096L;

    @Unique
    private static final long TRANSFER_READ = 2048L;

    @Unique
    private static final long ALL_COMMANDS = 65536L;

    @Unique
    private static final long MEMORY_READ_WRITE = 98304L;

    @Unique
    private static final int LAYOUT_UNDEFINED = 0;

    @Unique
    private static final int LAYOUT_GENERAL = 1;

    @Unique
    private static final int LAYOUT_TRANSFER_DESTINATION = 7;

    @Unique
    private static final int LAYOUT_PRESENT = 1000001002;

    @Unique
    private static final int COLOR_ASPECT = 1;

    @Unique
    private static final int NEAREST_FILTER = 0;

    @Shadow
    @Final
    private VulkanDevice device;

    @Shadow
    private long swapchain;

    @Shadow
    private int swapchainWidth;

    @Shadow
    private int swapchainHeight;

    @Shadow
    @Final
    private LongList swapchainImages;

    @Shadow
    @Final
    private long[] acquireSemaphores;

    @Shadow
    private int currentAcquireSemaphore;

    @Shadow
    private long[] presentSemaphores;

    @Shadow
    private int currentImageIndex;

    @Shadow
    private boolean swapchainOutOfDate;

    @Unique
    private long txoptimizations$blitPool;

    @Unique
    private VkCommandBuffer[] txoptimizations$blits = new VkCommandBuffer[0];

    @Unique
    private long[] txoptimizations$blitSwapchains = new long[0];

    @Unique
    private long[] txoptimizations$blitSources = new long[0];

    @Unique
    private Object[] txoptimizations$blitTextures = new Object[0];

    @Unique
    private int[] txoptimizations$blitMips = new int[0];

    @Unique
    private int[] txoptimizations$blitWidths = new int[0];

    @Unique
    private int[] txoptimizations$blitHeights = new int[0];

    @Inject(method = "blitFromTexture", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$reuseBlit(CommandEncoderBackend commandEncoder, GpuTextureView textureView, CallbackInfo ci) {
        if (GpuPassProfiler.isRunning() || this.swapchainOutOfDate)
            return;

        int index = this.currentImageIndex;

        if (index == -1)
            throw new IllegalStateException("No swapchain image is acquired for the screen copy");

        VulkanGpuTexture texture = (VulkanGpuTexture) textureView.texture();
        long             source  = texture.vkImage();
        int  mip    = textureView.baseMipLevel();
        int  width  = Math.min(this.swapchainWidth, textureView.getWidth(0));
        int  height = Math.min(this.swapchainHeight, textureView.getHeight(0));

        VulkanCommandEncoder encoder = (VulkanCommandEncoder) commandEncoder;
        VkCommandBuffer      blit    = this.txoptimizations$blit(index, texture, source, mip, width, height);

        encoder.waitSemaphore(this.acquireSemaphores[this.currentAcquireSemaphore], 0L, ALL_COMMANDS);
        encoder.execute(blit);
        encoder.signalSemaphore(this.presentSemaphores[index], 0L, TRANSFER_STAGE);
        ci.cancel();
    }

    @Inject(method = {"configure", "close"}, at = @At("HEAD"))
    private void txoptimizations$forgetBlits(CallbackInfo ci) {
        if (this.txoptimizations$blitPool == 0L)
            return;

        PresentThread.drain();

        VkDevice vkDevice = this.device.vkDevice();

        VulkanUtils.crashIfFailure(this.device, VK12.vkDeviceWaitIdle(vkDevice), "Failed to wait for the device before dropping screen copies");
        VK12.vkDestroyCommandPool(vkDevice, this.txoptimizations$blitPool, null);
        this.txoptimizations$blitPool     = 0L;
        this.txoptimizations$blits        = new VkCommandBuffer[0];
        this.txoptimizations$blitTextures = new Object[0];
    }

    @Unique
    private VkCommandBuffer txoptimizations$blit(int index, VulkanGpuTexture texture, long source, int mip, int width, int height) {
        if (this.txoptimizations$blits.length != this.swapchainImages.size())
            this.txoptimizations$allocateBlits();

        VkCommandBuffer blit = this.txoptimizations$blits[index];

        if (this.txoptimizations$blitSwapchains[index] == this.swapchain && this.txoptimizations$blitTextures[index] == texture && this.txoptimizations$blitSources[index] == source && this.txoptimizations$blitMips[index] == mip && this.txoptimizations$blitWidths[index] == width && this.txoptimizations$blitHeights[index] == height)
            return blit;

        if (this.txoptimizations$blitSwapchains[index] != 0L)
            VulkanUtils.crashIfFailure(this.device, VK12.vkDeviceWaitIdle(this.device.vkDevice()), "Failed to wait for the device before recording a screen copy");

        this.txoptimizations$record(blit, this.swapchainImages.getLong(index), source, mip, width, height);
        this.txoptimizations$blitSwapchains[index] = this.swapchain;
        this.txoptimizations$blitTextures[index]   = texture;
        this.txoptimizations$blitSources[index]    = source;
        this.txoptimizations$blitMips[index]       = mip;
        this.txoptimizations$blitWidths[index]     = width;
        this.txoptimizations$blitHeights[index]    = height;

        return blit;
    }

    @Unique
    private void txoptimizations$allocateBlits() {
        VkDevice vkDevice = this.device.vkDevice();
        int      count    = this.swapchainImages.size();

        if (this.txoptimizations$blitPool != 0L)
            throw new IllegalStateException("Screen copies were allocated for " + this.txoptimizations$blits.length + " swapchain images but the swapchain has " + count);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack).sType$Default();

            poolInfo.flags(RESET_COMMAND_BUFFER);
            poolInfo.queueFamilyIndex(this.device.graphicsQueue().queueFamilyIndex());

            LongBuffer pool = stack.mallocLong(1);

            VulkanUtils.crashIfFailure(this.device, VK12.vkCreateCommandPool(vkDevice, poolInfo, null, pool), "Failed to create the screen copy command pool");
            this.txoptimizations$blitPool = pool.get(0);

            VkCommandBufferAllocateInfo allocateInfo = VkCommandBufferAllocateInfo.calloc(stack).sType$Default();

            allocateInfo.commandPool(this.txoptimizations$blitPool);
            allocateInfo.level(VK12.VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocateInfo.commandBufferCount(count);

            PointerBuffer handles = stack.mallocPointer(count);

            VulkanUtils.crashIfFailure(this.device, VK12.vkAllocateCommandBuffers(vkDevice, allocateInfo, handles), "Failed to allocate the screen copy command buffers");

            VkCommandBuffer[] blits = new VkCommandBuffer[count];

            for (int image = 0; image < count; image ++)
                blits[image] = new VkCommandBuffer(handles.get(image), vkDevice);

            this.txoptimizations$blits          = blits;
            this.txoptimizations$blitSwapchains = new long[count];
            this.txoptimizations$blitTextures   = new Object[count];
            this.txoptimizations$blitSources    = new long[count];
            this.txoptimizations$blitMips       = new int[count];
            this.txoptimizations$blitWidths     = new int[count];
            this.txoptimizations$blitHeights    = new int[count];
        }
    }

    @Unique
    private void txoptimizations$record(VkCommandBuffer blit, long swapchainImage, long source, int mip, int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack).sType$Default();

            beginInfo.flags(SIMULTANEOUS_USE);
            VulkanUtils.crashIfFailure(this.device, VK12.vkResetCommandBuffer(blit, 0), "Failed to reset the screen copy command buffer");
            VulkanUtils.crashIfFailure(this.device, VK12.vkBeginCommandBuffer(blit, beginInfo), "Failed to begin the screen copy command buffer");

            VkImageMemoryBarrier2.Buffer toTransfer = VkImageMemoryBarrier2.calloc(1, stack).sType$Default();

            toTransfer.srcStageMask(0L);
            toTransfer.srcAccessMask(0L);
            toTransfer.dstStageMask(TRANSFER_STAGE);
            toTransfer.dstAccessMask(TRANSFER_WRITE);
            toTransfer.oldLayout(LAYOUT_UNDEFINED);
            toTransfer.newLayout(LAYOUT_TRANSFER_DESTINATION);
            toTransfer.srcQueueFamilyIndex(-1);
            toTransfer.dstQueueFamilyIndex(-1);
            toTransfer.image(swapchainImage);
            txoptimizations$colorRange(toTransfer.subresourceRange());
            KHRSynchronization2.vkCmdPipelineBarrier2KHR(blit, VkDependencyInfo.calloc(stack).sType$Default().pImageMemoryBarriers(toTransfer));

            VkOffset3D.Buffer sourceOffsets      = VkOffset3D.calloc(2, stack);
            VkOffset3D.Buffer destinationOffsets = VkOffset3D.calloc(2, stack);

            sourceOffsets.get(0).set(0, 0, 0);
            sourceOffsets.get(1).set(width, height, 1);
            destinationOffsets.get(0).set(0, height, 0);
            destinationOffsets.get(1).set(width, 0, 1);

            VkImageBlit.Buffer region = VkImageBlit.calloc(1, stack);

            region.srcSubresource(txoptimizations$colorLayers(VkImageSubresourceLayers.calloc(stack), mip));
            region.srcOffsets(sourceOffsets);
            region.dstSubresource(txoptimizations$colorLayers(VkImageSubresourceLayers.calloc(stack), 0));
            region.dstOffsets(destinationOffsets);
            VK12.vkCmdBlitImage(blit, source, LAYOUT_GENERAL, swapchainImage, LAYOUT_TRANSFER_DESTINATION, region, NEAREST_FILTER);

            VkImageMemoryBarrier2.Buffer toPresent = VkImageMemoryBarrier2.calloc(1, stack).sType$Default();

            toPresent.srcStageMask(TRANSFER_STAGE);
            toPresent.srcAccessMask(TRANSFER_WRITE);
            toPresent.dstStageMask(ALL_COMMANDS);
            toPresent.dstAccessMask(0L);
            toPresent.oldLayout(LAYOUT_TRANSFER_DESTINATION);
            toPresent.newLayout(LAYOUT_PRESENT);
            toPresent.srcQueueFamilyIndex(-1);
            toPresent.dstQueueFamilyIndex(-1);
            toPresent.image(swapchainImage);
            txoptimizations$colorRange(toPresent.subresourceRange());

            VkMemoryBarrier2.Buffer memory = VkMemoryBarrier2.calloc(1, stack).sType$Default();

            memory.srcStageMask(TRANSFER_STAGE);
            memory.srcAccessMask(TRANSFER_READ);
            memory.dstStageMask(ALL_COMMANDS);
            memory.dstAccessMask(MEMORY_READ_WRITE);
            KHRSynchronization2.vkCmdPipelineBarrier2KHR(blit, VkDependencyInfo.calloc(stack).sType$Default().pMemoryBarriers(memory).pImageMemoryBarriers(toPresent));
            VulkanUtils.crashIfFailure(this.device, VK12.vkEndCommandBuffer(blit), "Failed to end the screen copy command buffer");
        }
    }

    @Unique
    private static void txoptimizations$colorRange(VkImageSubresourceRange range) {
        range.aspectMask(COLOR_ASPECT);
        range.baseMipLevel(0);
        range.levelCount(1);
        range.baseArrayLayer(0);
        range.layerCount(1);
    }

    @Unique
    private static VkImageSubresourceLayers txoptimizations$colorLayers(VkImageSubresourceLayers layers, int mip) {
        layers.aspectMask(COLOR_ASPECT);
        layers.mipLevel(mip);
        layers.baseArrayLayer(0);
        layers.layerCount(1);

        return layers;
    }

    @Shadow
    @Final
    private VkQueue presentQueue;

    @Shadow
    private SurfaceException eatenException;

    @Shadow
    private boolean swapchainSuboptimal;

    @Inject(method = {"close", "configure", "acquireNextTexture"}, at = @At("HEAD"))
    private void txoptimizations$drainPresentThread(CallbackInfo ci) {
        PresentThread.drain();
    }

    @Inject(method = "isSuboptimal", at = @At("HEAD"))
    private void txoptimizations$drainBeforeSuboptimal(CallbackInfoReturnable<Boolean> cir) {
        PresentThread.drain();
    }

    @ModifyArg(method = "configure", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VkSwapchainCreateInfoKHR;minImageCount(I)Lorg/lwjgl/vulkan/VkSwapchainCreateInfoKHR;"))
    private int txoptimizations$spareImmediateImage(int imageCount, @Local(argsOnly = true) GpuSurface.Configuration config, @Local VkSurfaceCapabilitiesKHR capabilities) {
        if (config.presentMode() != GpuSurface.PresentMode.IMMEDIATE)
            return imageCount;

        int maxImageCount = capabilities.maxImageCount();

        if (maxImageCount != 0 && imageCount + 1 > maxImageCount)
            return imageCount;

        return imageCount + 1;
    }

    @Inject(method = "present", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$handOffPresent(CallbackInfo ci) {
        if (!PresentThread.isCapturing()) {
            PresentThread.drain();

            return;
        }

        if (this.swapchainOutOfDate)
            throw new IllegalStateException("Attempt to use out of date swapchain");

        long semaphore  = this.presentSemaphores[this.currentImageIndex];
        long frameChain = this.swapchain;
        int  imageIndex = this.currentImageIndex;

        this.currentImageIndex = -1;
        PresentThread.capture(() -> this.txoptimizations$queuePresent(semaphore, frameChain, imageIndex));
        ci.cancel();
    }

    @Override
    public void txoptimizations$discardAcquired() {
        if (this.currentImageIndex == -1)
            throw new IllegalStateException("No swapchain image is acquired");

        try (VulkanQueue.Submission submission = this.device.graphicsQueue().beginSubmit()) {
            submission.waitSemaphore(this.acquireSemaphores[this.currentAcquireSemaphore], 0L, VK13.VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT);
        }

        this.currentImageIndex = -1;
    }

    @Unique
    private void txoptimizations$queuePresent(long semaphore, long frameChain, int imageIndex) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack).sType$Default();

            presentInfo.pWaitSemaphores(stack.longs(semaphore));
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(frameChain));
            presentInfo.pImageIndices(stack.ints(imageIndex));

            int result = KHRSwapchain.vkQueuePresentKHR(this.presentQueue, presentInfo);

            if (result == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR) {
                this.swapchainSuboptimal = true;
                this.swapchainOutOfDate  = true;
                this.eatenException      = new SurfaceException("Failed to present image, swapchain out of date");
            } else if (result == KHRSwapchain.VK_SUBOPTIMAL_KHR) {
                this.swapchainSuboptimal = true;
            } else {
                VulkanUtils.crashIfFailure(this.device, result, "Failed to present image");
            }
        }
    }
}
