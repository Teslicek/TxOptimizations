package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPass;
import com.teslicek.txoptimizations.DynamicStateCache;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkOffset2D;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkViewport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = VulkanRenderPass.class, priority = 999)
public abstract class VulkanRenderPassDynamicStateMixin {

    @WrapWithCondition(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VK12;vkCmdSetViewport(Lorg/lwjgl/vulkan/VkCommandBuffer;ILorg/lwjgl/vulkan/VkViewport$Buffer;)V"))
    private boolean txoptimizations$setChangedViewport(VkCommandBuffer commandBuffer, int firstViewport, VkViewport.Buffer viewports) {
        VkViewport viewport = viewports.get(0);

        if (firstViewport != 0 || viewports.remaining() != 1 || viewport.x() != 0.0F || viewport.y() != 0.0F || viewport.minDepth() != 0.0F || viewport.maxDepth() != 1.0F)
            throw new IllegalStateException("Unexpected viewport layout");

        return DynamicStateCache.changesViewport(commandBuffer.address(), viewport.width(), viewport.height());
    }

    @Overwrite
    private static void setScissor(MemoryStack stack, VkCommandBuffer commandBuffer, int x, int y, int width, int height) {
        if (!DynamicStateCache.changesScissor(commandBuffer.address(), x, y, width, height))
            return;

        long scissor = stack.ncalloc(VkRect2D.ALIGNOF, 1, VkRect2D.SIZEOF);

        VkOffset2D.nx(scissor + VkRect2D.OFFSET, x);
        VkOffset2D.ny(scissor + VkRect2D.OFFSET, y);
        VkExtent2D.nwidth(scissor + VkRect2D.EXTENT, width);
        VkExtent2D.nheight(scissor + VkRect2D.EXTENT, height);
        VK10.nvkCmdSetScissor(commandBuffer, 0, 1, scissor);
    }
}
