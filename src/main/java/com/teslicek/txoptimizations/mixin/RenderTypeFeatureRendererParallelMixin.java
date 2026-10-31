package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ParallelFeatures;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderTypeFeatureRenderer.class)
public abstract class RenderTypeFeatureRendererParallelMixin {

    @Redirect(method = "prepareGroup", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", ordinal = 0), require = 1)
    private Iterator<Object> txoptimizations$requestOnlyNewGroup(List<Object> groups) {
        return List.of(groups.getLast()).iterator();
    }

    @Redirect(method = "prepareGroup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/StagedVertexBuffer;requestIndexCount(Lnet/minecraft/client/renderer/StagedVertexBuffer$Draw;)V"))
    private void txoptimizations$requestOnRenderThread(StagedVertexBuffer buffer, StagedVertexBuffer.Draw draw) {
        if (ParallelFeatures.isWorkerThread())
            return;

        buffer.requestIndexCount(draw);
    }
}
