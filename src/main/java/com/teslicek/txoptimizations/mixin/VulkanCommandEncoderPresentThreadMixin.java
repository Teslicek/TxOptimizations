package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.mojang.renderpearl.backend.vulkan.VulkanQueue;
import com.teslicek.txoptimizations.PresentThread;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VulkanCommandEncoder.class)
public abstract class VulkanCommandEncoderPresentThreadMixin {

    @WrapOperation(method = "submit", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanQueue$Submission;close()V"))
    private void txoptimizations$submitOnPresentThread(VulkanQueue.Submission submission, Operation<Void> original) {
        if (!PresentThread.isArmed()) {
            original.call(submission);

            return;
        }

        PresentThread.handOff(submission);
    }
}
