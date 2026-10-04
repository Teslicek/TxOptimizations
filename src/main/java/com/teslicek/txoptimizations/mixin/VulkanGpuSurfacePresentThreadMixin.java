package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.device.GpuSurface;
import com.mojang.renderpearl.api.device.SurfaceException;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuSurface;
import com.mojang.renderpearl.backend.vulkan.VulkanQueue;
import com.mojang.renderpearl.backend.vulkan.VulkanUtils;
import com.teslicek.txoptimizations.PresentThread;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK13;
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
public abstract class VulkanGpuSurfacePresentThreadMixin {

    @Shadow
    @Final
    private VulkanDevice device;

    @Shadow
    @Final
    private VkQueue presentQueue;

    @Shadow
    private long swapchain;

    @Shadow
    private long[] presentSemaphores;

    @Shadow
    private int currentImageIndex;

    @Shadow
    private SurfaceException eatenException;

    @Shadow
    private boolean swapchainSuboptimal;

    @Shadow
    private boolean swapchainOutOfDate;

    @Shadow
    @Final
    private long[] acquireSemaphores;

    @Shadow
    private int currentAcquireSemaphore;

    @Unique
    private boolean txoptimizations$acquiredAhead;

    @Unique
    private SurfaceException txoptimizations$acquireAheadFailure;

    @Inject(method = {"close", "configure"}, at = @At("HEAD"))
    private void txoptimizations$drainAndReleaseAhead(CallbackInfo ci) {
        PresentThread.drain();

        if (!this.txoptimizations$acquiredAhead)
            return;

        this.txoptimizations$acquiredAhead = false;

        if (this.txoptimizations$acquireAheadFailure != null) {
            this.txoptimizations$acquireAheadFailure = null;

            return;
        }

        try (VulkanQueue.Submission release = this.device.graphicsQueue().beginSubmit()) {
            release.waitSemaphore(this.acquireSemaphores[this.currentAcquireSemaphore], 0L, VK13.VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT);
        }

        this.currentImageIndex = -1;
    }

    @Inject(method = "acquireNextTexture", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$takeAcquiredAhead(CallbackInfo ci) throws SurfaceException {
        if (PresentThread.isWorker())
            return;

        PresentThread.drain();

        if (!this.txoptimizations$acquiredAhead)
            return;

        SurfaceException failure = this.txoptimizations$acquireAheadFailure;

        this.txoptimizations$acquiredAhead       = false;
        this.txoptimizations$acquireAheadFailure = null;
        ci.cancel();

        if (failure != null)
            throw failure;
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
        boolean acquireAhead = PresentThread.acquiresAhead();

        PresentThread.capture(() -> {
            this.txoptimizations$queuePresent(semaphore, frameChain, imageIndex);

            if (acquireAhead)
                this.txoptimizations$acquireAhead();
        });
        ci.cancel();
    }

    @Unique
    private void txoptimizations$acquireAhead() {
        if (this.swapchainOutOfDate || this.eatenException != null)
            return;

        try {
            ((VulkanGpuSurface) (Object) this).acquireNextTexture();
        } catch (SurfaceException failure) {
            this.txoptimizations$acquireAheadFailure = failure;
        }

        this.txoptimizations$acquiredAhead = true;
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
