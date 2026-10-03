package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanQueue;
import com.mojang.renderpearl.backend.vulkan.VulkanTransientMemory;
import com.teslicek.txoptimizations.PresentThread;
import com.teslicek.txoptimizations.SceneSubmit;
import com.teslicek.txoptimizations.TransientCopies;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VulkanCommandEncoder.class)
public abstract class VulkanCommandEncoderPresentThreadMixin implements SceneSubmit {

    @Shadow
    @Final
    private VulkanDevice device;

    @Shadow
    @Final
    private VulkanTransientMemory transientMemory;

    @Shadow
    private VulkanQueue.Submission submissionBuilder;

    @Shadow
    protected abstract void endCommandBuffer();

    @WrapOperation(method = "submit", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanQueue$Submission;close()V"))
    private void txoptimizations$submitOnPresentThread(VulkanQueue.Submission submission, Operation<Void> original) {
        if (!PresentThread.isArmed()) {
            original.call(submission);

            return;
        }

        PresentThread.handOff(submission);
    }

    @Override
    public void txoptimizations$submitScene() {
        this.endCommandBuffer();
        ((TransientCopies) this.transientMemory).txoptimizations$endCopies();

        VulkanQueue.Submission scene = this.submissionBuilder;

        this.submissionBuilder = this.device.graphicsQueue().beginSubmit();
        PresentThread.submit(scene);
    }
}
