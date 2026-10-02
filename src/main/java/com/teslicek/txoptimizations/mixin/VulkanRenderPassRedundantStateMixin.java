package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPass;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPipeline;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import java.util.Objects;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VulkanRenderPass.class)
public abstract class VulkanRenderPassRedundantStateMixin {

    @Shadow
    protected VulkanRenderPipeline pipeline;

    @Shadow
    @Final
    protected ReferenceList<Object> uniforms;

    @Inject(method = "setPipeline", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$skipBoundPipeline(BackendRenderPipeline pipeline, CallbackInfo ci) {
        if (pipeline == this.pipeline)
            ci.cancel();
    }

    @Inject(method = "setUniform", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$skipUnchangedUniform(int index, Object value, CallbackInfo ci) {
        if (Objects.equals(this.uniforms.get(index), value))
            ci.cancel();
    }
}
