package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPass;
import com.teslicek.txoptimizations.DynamicStateCache;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VulkanRenderPass.class)
public abstract class VulkanRenderPassBindSkipMixin {

    @WrapWithCondition(method = "setPipeline", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VK12;vkCmdBindPipeline(Lorg/lwjgl/vulkan/VkCommandBuffer;IJ)V"))
    private boolean txoptimizations$bindChangedPipeline(VkCommandBuffer commandBuffer, int bindPoint, long pipeline) {
        return DynamicStateCache.changesPipeline(commandBuffer.address(), pipeline);
    }
}
