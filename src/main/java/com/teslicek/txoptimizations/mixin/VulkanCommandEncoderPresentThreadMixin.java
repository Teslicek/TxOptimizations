package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.mojang.renderpearl.backend.vulkan.VulkanQueue;
import com.teslicek.txoptimizations.FramePath;
import com.teslicek.txoptimizations.PresentThread;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(VulkanCommandEncoder.class)
public abstract class VulkanCommandEncoderPresentThreadMixin {

    @Shadow
    private boolean awaitSubmitCompletion(long submitIndex, long timeoutNS) {
        throw new AssertionError();
    }

    @Redirect(method = "submit", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanCommandEncoder;awaitSubmitCompletion(JJ)Z"))
    private boolean txoptimizations$measureGpuWait(VulkanCommandEncoder encoder, long submitIndex, long timeout) {
        long    start  = System.nanoTime();
        boolean result = ((VulkanCommandEncoderPresentThreadMixin) (Object) encoder).awaitSubmitCompletion(submitIndex, timeout);

        FramePath.recordGpuWait(System.nanoTime() - start);

        return result;
    }

    @Redirect(method = "submit", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanQueue$Submission;close()V"))
    private void txoptimizations$submitOnPresentThread(VulkanQueue.Submission submission) {
        if (!PresentThread.isArmed()) {
            submission.close();

            return;
        }

        PresentThread.handOff(submission);
    }
}
