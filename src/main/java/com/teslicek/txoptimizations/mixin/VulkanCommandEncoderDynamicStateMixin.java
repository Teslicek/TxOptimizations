package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.teslicek.txoptimizations.DynamicStateCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VulkanCommandEncoder.class)
public abstract class VulkanCommandEncoderDynamicStateMixin {

    @Inject(method = "allocateAndBeginTransientCommandBuffer", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VK12;vkBeginCommandBuffer(Lorg/lwjgl/vulkan/VkCommandBuffer;Lorg/lwjgl/vulkan/VkCommandBufferBeginInfo;)I"))
    private void txoptimizations$forgetDynamicState(CallbackInfoReturnable<?> cir) {
        DynamicStateCache.reset();
    }
}
