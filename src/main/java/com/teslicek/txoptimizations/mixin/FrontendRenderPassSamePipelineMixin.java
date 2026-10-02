package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.frontend.FrontendRenderPass;
import com.mojang.renderpearl.frontend.FrontendRenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FrontendRenderPass.class)
public abstract class FrontendRenderPassSamePipelineMixin {

    @Shadow
    private FrontendRenderPipeline boundPipeline;

    @Inject(method = "setPipeline", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$keepBoundPipeline(CompiledRenderPipeline pipeline, CallbackInfo ci) {
        if (pipeline == this.boundPipeline)
            ci.cancel();
    }
}
