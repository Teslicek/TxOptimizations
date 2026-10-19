package com.teslicek.txoptimizations.mixin.gpu;

import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.backend.api.RenderPassBackend;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPass;
import com.mojang.renderpearl.frontend.FrontendRenderPass;
import com.mojang.renderpearl.frontend.FrontendRenderPipeline;
import com.teslicek.txoptimizations.gpu.FragmentCounts;
import com.teslicek.txoptimizations.gpu.GpuPassProfiler;
import com.teslicek.txoptimizations.gpu.ProfileCounters;
import java.nio.IntBuffer;
import java.util.Collection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FrontendRenderPass.class)
public abstract class FrontendRenderPassGpuProfilerMixin {

    @Shadow
    @Final
    private RenderPassBackend backend;

    @ModifyVariable(method = "setPipeline", at = @At("HEAD"), argsOnly = true)
    private CompiledRenderPipeline txoptimizations$profilePipeline(CompiledRenderPipeline pipeline) {
        if (GpuPassProfiler.isRunning())
            GpuPassProfiler.markPipeline((FrontendRenderPipeline) pipeline);

        if (GpuPassProfiler.isRecording() && this.backend instanceof VulkanRenderPass vulkanPass)
            FragmentCounts.begin(((VulkanRenderPassProfilerAccessor) vulkanPass).txoptimizations$device().vkDevice(), ((VulkanRenderPassProfilerAccessor) vulkanPass).txoptimizations$commandBuffer(), ((FrontendRenderPipeline) pipeline).name());

        ProfileCounters.countPipelineSet();

        return pipeline;
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void txoptimizations$endFragmentCount(CallbackInfo ci) {
        FragmentCounts.end();
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
