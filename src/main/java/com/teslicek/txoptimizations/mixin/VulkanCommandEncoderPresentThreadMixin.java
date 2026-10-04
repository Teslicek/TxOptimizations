package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.mojang.renderpearl.backend.vulkan.VulkanQueue;
import com.teslicek.txoptimizations.FramePath;
import com.teslicek.txoptimizations.PresentThread;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VulkanCommandEncoder.class)
public abstract class VulkanCommandEncoderPresentThreadMixin {

    @WrapOperation(method = "submit", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanCommandEncoder;awaitSubmitCompletion(JJ)Z"))
    private boolean txoptimizations$measureGpuWait(VulkanCommandEncoder encoder, long submitIndex, long timeout, Operation<Boolean> original) {
        long    start  = System.nanoTime();
        boolean result = original.call(encoder, submitIndex, timeout);

        FramePath.recordGpuWait(System.nanoTime() - start);

        return result;
    }

    @WrapOperation(method = "submit", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanQueue$Submission;close()V"))
    private void txoptimizations$submitOnPresentThread(VulkanQueue.Submission submission, Operation<Void> original) {
        if (!PresentThread.isArmed()) {
            original.call(submission);

            return;
        }

        PresentThread.handOff(submission);
    }
}
