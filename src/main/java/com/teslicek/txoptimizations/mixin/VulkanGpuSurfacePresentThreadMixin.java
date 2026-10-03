package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.device.SurfaceException;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuSurface;
import com.mojang.renderpearl.backend.vulkan.VulkanUtils;
import com.teslicek.txoptimizations.PresentThread;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkQueue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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

    @Inject(method = {"close", "configure"}, at = @At("HEAD"))
    private void txoptimizations$drainPresentThread(CallbackInfo ci) {
        PresentThread.drain();
    }

    @Inject(method = "acquireNextTexture", at = @At("HEAD"))
    private void txoptimizations$drainPresentsBeforeAcquire(CallbackInfo ci) {
        PresentThread.drainPresents();
    }

    @Inject(method = "isSuboptimal", at = @At("HEAD"))
    private void txoptimizations$drainPresentsBeforeSuboptimal(CallbackInfoReturnable<Boolean> cir) {
        PresentThread.drainPresents();
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
