package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.ParallelFeatures;
import java.util.List;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRendererMap;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FeatureRenderDispatcher.class)
public abstract class FeatureRenderDispatcherParallelMixin {

    @Shadow
    @Final
    private StagedVertexBuffer stagedVertexBuffer;

    @Shadow
    @Final
    private FeatureRendererMap featureRenderers;

    @Inject(method = "prepareFrameWithContext", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", ordinal = 1, shift = At.Shift.AFTER))
    private void txoptimizations$startWorker(FeatureFrameContext context, SubmitNodeStorage submitNodeStorage, CallbackInfoReturnable<FeatureRenderDispatcher.PreparedFrame> cir, @Local(ordinal = 0) FeatureRenderDispatcher.PreparedFrame frame) {
        FeaturePreparedFrameAccessor accessor = (FeaturePreparedFrameAccessor) frame;

        ParallelFeatures.start(context, accessor.txoptimizations$groupsByFeature(), this.featureRenderers, accessor.txoptimizations$allSubmits());
    }

    @Redirect(method = "prepareFrameWithContext", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedGroup;prepare(Lnet/minecraft/client/renderer/feature/FeatureFrameContext;Lnet/minecraft/client/renderer/feature/FeatureRendererMap;Ljava/util/List;)V"))
    private void txoptimizations$prepareOnRenderThread(@Coerce Object group, FeatureFrameContext context, FeatureRendererMap renderers, List<SubmitNode> submits) {
        if (ParallelFeatures.runsOnWorker(group))
            return;

        ((FeaturePreparedGroupAccessor) group).txoptimizations$prepare(context, renderers, submits);
    }

    @Inject(method = "prepareFrameWithContext", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", ordinal = 2))
    private void txoptimizations$joinWorker(FeatureFrameContext context, SubmitNodeStorage submitNodeStorage, CallbackInfoReturnable<FeatureRenderDispatcher.PreparedFrame> cir) {
        ParallelFeatures.finish(this.stagedVertexBuffer);
    }
}
