package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPipeline;
import com.teslicek.txoptimizations.PipelineLayoutKey;
import com.teslicek.txoptimizations.PipelineLayouts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VulkanRenderPipeline.class)
public abstract class VulkanRenderPipelineLayoutKeyMixin implements PipelineLayoutKey {

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
