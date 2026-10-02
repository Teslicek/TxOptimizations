package com.teslicek.txoptimizations.mixin.gpu;

import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.frontend.FrontendRenderPass;
import com.mojang.renderpearl.frontend.FrontendRenderPipeline;
import com.teslicek.txoptimizations.gpu.GpuPassProfiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FrontendRenderPass.class)
public abstract class FrontendRenderPassGpuProfilerMixin {

    @Inject(method = "setPipeline", at = @At("HEAD"))
    private void txoptimizations$profilePipeline(CompiledRenderPipeline pipeline, CallbackInfo ci) {
        if (GpuPassProfiler.isRunning())
            GpuPassProfiler.markPipeline(((FrontendRenderPipeline) pipeline).name());
    }
}
