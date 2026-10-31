package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPipeline;
import com.teslicek.txoptimizations.DepthBounds;
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VulkanRenderPipeline.class)
public abstract class VulkanRenderPipelineDepthBoundsMixin {

    @WrapOperation(method = "compile", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VkPipelineDepthStencilStateCreateInfo;depthCompareOp(I)Lorg/lwjgl/vulkan/VkPipelineDepthStencilStateCreateInfo;"))
    private static VkPipelineDepthStencilStateCreateInfo txoptimizations$keepToFarPlane(VkPipelineDepthStencilStateCreateInfo state, int compareOp, Operation<VkPipelineDepthStencilStateCreateInfo> original, @Local(argsOnly = true) BackendRenderPipeline.CreateInfo info) {
        VkPipelineDepthStencilStateCreateInfo result = original.call(state, compareOp);

        if (!DepthBounds.farOnly(info.name()))
            return result;

        if (!DepthBounds.enabled())
            throw new IllegalStateException(info.name() + " needs the depth bounds test, which the device did not enable");

        return result.depthBoundsTestEnable(true).minDepthBounds(DepthBounds.FAR).maxDepthBounds(DepthBounds.FAR);
    }
}
