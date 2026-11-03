package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.commands.RenderPass;
import com.teslicek.txoptimizations.PhaseGroups;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRendererMap;
import net.minecraft.client.renderer.feature.FeatureRendererType;
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

@Mixin(value = FeatureRenderDispatcher.PreparedFrame.class, priority = 999)
public abstract class FeaturePreparedFrameIndexedMixin {

    @Shadow
    @Final
    private List<SubmitNode> allSubmits;

    @Shadow
    private @Nullable SubmitNodeStorage submitNodeStorage;

    @Shadow
    @Final
    FeatureRenderDispatcher this$0;

    @Overwrite
    private void executePhase(FeatureRenderPhase<?> phase, FeatureFrameContext context, @Nullable OitStage stage, RenderPass renderPass) {
        List<?> groups = PhaseGroups.require(phase).txoptimizations$groups();

        if (groups == null || groups.isEmpty())
            return;

        ProfilerFiller     profiler  = Profiler.get();
        FeatureRendererMap renderers = ((FeatureRenderDispatcherRenderersAccessor) this.this$0).txoptimizations$featureRenderers();

        for (int index = 0; index < groups.size(); index ++) {
            FeaturePreparedGroupAccessor group       = (FeaturePreparedGroupAccessor) groups.get(index);
            FeatureRendererType<?>       featureType = group.txoptimizations$featureType();

            profiler.push(featureType.toString());
            renderPass.pushDebugGroup((Supplier<String>) (Object) featureType);
            group.txoptimizations$execute(context, stage, renderPass, renderers, this.allSubmits);
            renderPass.popDebugGroup();
            profiler.pop();
        }
    }

    @Overwrite
    public boolean hasAnyOutline() {
        for (SubmitNodeCollection collection : Objects.requireNonNull(this.submitNodeStorage).getSubmitsPerOrder().values()) {
            if (PhaseGroups.hasGroups(collection.outline))
                return true;
        }

        return false;
    }

    @Overwrite
    public boolean hasAnyWaterMask() {
        for (SubmitNodeCollection collection : Objects.requireNonNull(this.submitNodeStorage).getSubmitsPerOrder().values()) {
            if (PhaseGroups.hasGroups(collection.waterMask))
                return true;
        }

        return false;
    }

    @Overwrite
    public boolean hasAnySeeThrough() {
        return PhaseGroups.hasGroups(Objects.requireNonNull(this.submitNodeStorage).seeThrough());
    }
}
