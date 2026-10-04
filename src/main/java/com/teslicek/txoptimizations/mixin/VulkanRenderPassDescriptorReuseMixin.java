package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPass;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPipeline;
import com.teslicek.txoptimizations.DynamicStateCache;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VulkanRenderPass.class)
public abstract class VulkanRenderPassDescriptorReuseMixin {

    @Shadow
    protected VulkanRenderPipeline pipeline;

    @Shadow
    private boolean anyDescriptorDirty;

    @Shadow
    @Final
    protected ReferenceList<Object> uniforms;

    @Shadow
    @Final
    private VkCommandBuffer commandBuffer;

    @WrapWithCondition(method = {"drawIndexed", "multiDrawIndexed", "drawIndexedIndirect", "draw", "multiDraw", "drawIndirect"}, at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanRenderPass;pushDescriptors()V"))
    private boolean txoptimizations$pushChangedDescriptors(VulkanRenderPass pass) {
        if (!this.anyDescriptorDirty)
            return false;

        if (DynamicStateCache.changesDescriptors(this.commandBuffer.address(), this.pipeline.pipelineLayout(), this.uniforms))
            return true;

        this.anyDescriptorDirty = false;

        return false;
    }
}
