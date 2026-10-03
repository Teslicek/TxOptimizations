package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.mojang.renderpearl.backend.vulkan.VulkanTransientMemory;
import com.teslicek.txoptimizations.FrontSubmission;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VulkanTransientMemory.class)
public abstract class VulkanTransientMemoryLazyMixin {

    @Shadow
    @Final
    private VulkanCommandEncoder encoder;

    @Shadow
    private boolean anyCommandRecorded;

    @Shadow
    private VkCommandBuffer commandBuffer;

    @Overwrite
    public void beginSubmit() {
        if (this.commandBuffer != null)
            throw new IllegalStateException("Transient command buffer is still open");

        this.anyCommandRecorded = false;
    }

    @Inject(method = "recordGpuMappedCopy", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VkBufferCopy;calloc(ILorg/lwjgl/system/MemoryStack;)Lorg/lwjgl/vulkan/VkBufferCopy$Buffer;"))
    private void txoptimizations$beginOnFirstCopy(CallbackInfo ci) {
        if (this.commandBuffer != null)
            return;

        this.commandBuffer = this.encoder.allocateAndBeginTransientCommandBuffer();
        ((FrontSubmission) ((VulkanCommandEncoderSubmissionAccessor) this.encoder).txoptimizations$submissionBuilder()).txoptimizations$executeFirst(this.commandBuffer);
    }

    @WrapOperation(method = "endSubmit", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VK12;vkEndCommandBuffer(Lorg/lwjgl/vulkan/VkCommandBuffer;)I"))
    private int txoptimizations$endOnlyStarted(VkCommandBuffer commandBuffer, Operation<Integer> original) {
        if (commandBuffer == null)
            return VK10.VK_SUCCESS;

        return original.call(commandBuffer);
    }
}
