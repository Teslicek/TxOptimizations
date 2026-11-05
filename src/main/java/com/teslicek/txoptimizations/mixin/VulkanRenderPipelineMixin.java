package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPipeline;
import com.teslicek.txoptimizations.DepthBounds;
import com.teslicek.txoptimizations.PipelineLayoutKey;
import com.teslicek.txoptimizations.PipelineLayouts;
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VulkanRenderPipeline.class)
public abstract class VulkanRenderPipelineMixin implements PipelineLayoutKey {

    @WrapOperation(method = "compile", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VkPipelineDepthStencilStateCreateInfo;depthCompareOp(I)Lorg/lwjgl/vulkan/VkPipelineDepthStencilStateCreateInfo;"))
    private static VkPipelineDepthStencilStateCreateInfo txoptimizations$keepToFarPlane(VkPipelineDepthStencilStateCreateInfo state, int compareOp, Operation<VkPipelineDepthStencilStateCreateInfo> original, @Local(argsOnly = true) BackendRenderPipeline.CreateInfo info) {
        VkPipelineDepthStencilStateCreateInfo result = original.call(state, compareOp);

        if (!DepthBounds.farOnly(info.name()))
            return result;

        if (!DepthBounds.enabled())
            throw new IllegalStateException(info.name() + " needs the depth bounds test, which the device did not enable");

        return result.depthBoundsTestEnable(true).minDepthBounds(DepthBounds.FAR).maxDepthBounds(DepthBounds.FAR);
    }

    @Unique
    private long txoptimizations$layoutKey;

    @Inject(method = "compile", at = @At("RETURN"))
    private static void txoptimizations$keyLayout(VulkanDevice device, BackendRenderPipeline.CreateInfo createInfo, CallbackInfoReturnable<VulkanRenderPipeline> cir) {
        ((PipelineLayoutKey) (Object) cir.getReturnValue()).txoptimizations$setLayoutKey(PipelineLayouts.key(createInfo.uniforms(), createInfo.pushConstantsSize()));
    }

    @Override
    public long txoptimizations$layoutKey() {
        if (this.txoptimizations$layoutKey == 0L)
            throw new IllegalStateException("Pipeline was created without its layout key");

        return this.txoptimizations$layoutKey;
    }

    @Override
    public void txoptimizations$setLayoutKey(long key) {
        this.txoptimizations$layoutKey = key;
    }
}
