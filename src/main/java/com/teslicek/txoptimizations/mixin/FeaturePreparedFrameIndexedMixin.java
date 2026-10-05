package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.commands.RenderPass;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRendererMap;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FeatureRenderDispatcher.PreparedFrame.class)
public abstract class FeaturePreparedFrameIndexedMixin {

    @Shadow
    @Final
    private List<SubmitNode> allSubmits;

    @Shadow
    @Final
    private Map<FeatureRenderPhase<?>, List<?>> groupsByPhase;

    @Shadow
    @Final
    FeatureRenderDispatcher this$0;

    @Overwrite
    private void executePhase(FeatureRenderPhase<?> phase, FeatureFrameContext context, @Nullable OitStage stage, RenderPass renderPass) {
        List<?> groups = this.groupsByPhase.get(phase);

        if (groups == null)
            return;

        ProfilerFiller     profiler  = Profiler.get();
        FeatureRendererMap renderers = ((FeatureRenderDispatcherRenderersAccessor) this.this$0).txoptimizations$featureRenderers();

        for (int index = 0; index < groups.size(); index ++) {
            FeaturePreparedGroupAccessor group           = (FeaturePreparedGroupAccessor) groups.get(index);
            String                       featureTypeName = group.txoptimizations$featureType().toString();

            profiler.push(featureTypeName);
            renderPass.pushDebugGroup(() -> featureTypeName);
            group.txoptimizations$execute(context, stage, renderPass, renderers, this.allSubmits);
            renderPass.popDebugGroup();
            profiler.pop();
        }
    }
}
