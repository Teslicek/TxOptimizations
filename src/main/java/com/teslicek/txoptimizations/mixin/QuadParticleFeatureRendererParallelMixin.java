package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ParallelFeatures;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.feature.QuadParticleFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(QuadParticleFeatureRenderer.class)
public abstract class QuadParticleFeatureRendererParallelMixin {

    @Redirect(method = "prepareGroup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/StagedVertexBuffer;requestIndexCount(Lnet/minecraft/client/renderer/StagedVertexBuffer$Draw;)V"))
    private void txoptimizations$requestOnRenderThread(StagedVertexBuffer buffer, StagedVertexBuffer.Draw draw) {
        if (ParallelFeatures.isWorkerThread())
            return;

        buffer.requestIndexCount(draw);
    }
}
