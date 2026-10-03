package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPass;
import com.teslicek.txoptimizations.DynamicStateCache;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkViewport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VulkanRenderPass.class)
public abstract class VulkanRenderPassDynamicStateMixin {

    @WrapWithCondition(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VK12;vkCmdSetViewport(Lorg/lwjgl/vulkan/VkCommandBuffer;ILorg/lwjgl/vulkan/VkViewport$Buffer;)V"))
    private boolean txoptimizations$setChangedViewport(VkCommandBuffer commandBuffer, int firstViewport, VkViewport.Buffer viewports) {
        VkViewport viewport = viewports.get(0);

        if (firstViewport != 0 || viewports.remaining() != 1 || viewport.x() != 0.0F || viewport.y() != 0.0F || viewport.minDepth() != 0.0F || viewport.maxDepth() != 1.0F)
            throw new IllegalStateException("Unexpected viewport layout");

        return DynamicStateCache.changesViewport(commandBuffer.address(), viewport.width(), viewport.height());
    }

    @WrapWithCondition(method = "setScissor", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VK12;vkCmdSetScissor(Lorg/lwjgl/vulkan/VkCommandBuffer;ILorg/lwjgl/vulkan/VkRect2D$Buffer;)V"))
    private static boolean txoptimizations$setChangedScissor(VkCommandBuffer commandBuffer, int firstScissor, VkRect2D.Buffer scissors) {
        VkRect2D scissor = scissors.get(0);

        if (firstScissor != 0 || scissors.remaining() != 1)
            throw new IllegalStateException("Unexpected scissor layout");

        return DynamicStateCache.changesScissor(commandBuffer.address(), scissor.offset().x(), scissor.offset().y(), scissor.extent().width(), scissor.extent().height());
    }
}
