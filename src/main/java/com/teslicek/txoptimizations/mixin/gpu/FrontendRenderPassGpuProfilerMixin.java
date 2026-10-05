package com.teslicek.txoptimizations.mixin.gpu;

import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.frontend.FrontendRenderPass;
import com.mojang.renderpearl.frontend.FrontendRenderPipeline;
import com.teslicek.txoptimizations.gpu.GpuPassProfiler;
import com.teslicek.txoptimizations.gpu.ProfileCounters;
import java.nio.IntBuffer;
import java.util.Collection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FrontendRenderPass.class)
public abstract class FrontendRenderPassGpuProfilerMixin {

    @ModifyVariable(method = "setPipeline", at = @At("HEAD"), argsOnly = true)
    private CompiledRenderPipeline txoptimizations$profilePipeline(CompiledRenderPipeline pipeline) {
        if (GpuPassProfiler.isRunning())
            GpuPassProfiler.markPipeline((FrontendRenderPipeline) pipeline);

        ProfileCounters.countPipelineSet();

        return pipeline;
    }

    @ModifyVariable(method = {"drawIndexed", "draw"}, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int txoptimizations$countDraw(int first) {
        ProfileCounters.countDraw();

        return first;
    }

    @ModifyVariable(method = {"multiDrawIndexed", "multiDraw"}, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private IntBuffer txoptimizations$countMultiDraw(IntBuffer first) {
        ProfileCounters.countMultiDraw();

        return first;
    }

    @ModifyVariable(method = {"drawIndexedIndirect", "drawIndirect"}, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int txoptimizations$countIndirectDraws(int count) {
        ProfileCounters.countIndirectDraws(count);

        return count;
    }

    @ModifyVariable(method = "drawMultipleIndexed", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Collection<?> txoptimizations$countBatchedDraws(Collection<?> draws) {
        ProfileCounters.countBatchedDraws(draws.size());

        return draws;
    }
}
