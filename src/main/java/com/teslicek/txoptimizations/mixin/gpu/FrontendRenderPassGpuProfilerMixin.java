package com.teslicek.txoptimizations.mixin.gpu;

import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.frontend.FrontendRenderPass;
import com.mojang.renderpearl.frontend.FrontendRenderPipeline;
import com.teslicek.txoptimizations.gpu.GpuPassProfiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FrontendRenderPass.class)
public abstract class FrontendRenderPassGpuProfilerMixin {

    @ModifyVariable(method = "setPipeline", at = @At("HEAD"), argsOnly = true)
    private CompiledRenderPipeline txoptimizations$profilePipeline(CompiledRenderPipeline pipeline) {
        if (GpuPassProfiler.isRunning())
            GpuPassProfiler.markPipeline(((FrontendRenderPipeline) pipeline).name());

        return pipeline;
    }
}
