package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.commands.RenderPass;
import com.teslicek.txoptimizations.OutlineScissor;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FeatureRenderDispatcher.PreparedFrame.class)
public abstract class FeaturePreparedFrameOutlineMixin {

    @Shadow
    private FeatureFrameContext context;

    @Shadow
    private SubmitNodeStorage submitNodeStorage;

    @Shadow
    @Final
    private Map<FeatureRenderPhase<?>, List<?>> groupsByPhase;

    @Shadow
    @Final
    FeatureRenderDispatcher this$0;

    @Inject(method = "executeOutline", at = @At("HEAD"))
    private void txoptimizations$measureOutline(RenderPass renderPass, CallbackInfo ci) {
        if (this.submitNodeStorage == null || this.context == null)
            throw new IllegalStateException("The outline phase was executed outside a prepared frame");

        OutlineScissor.measure(this.submitNodeStorage, this.groupsByPhase, ((FeatureRenderDispatcherRenderersAccessor) this.this$0).txoptimizations$featureRenderers(), this.context.stagedVertexBuffer());
    }
}
