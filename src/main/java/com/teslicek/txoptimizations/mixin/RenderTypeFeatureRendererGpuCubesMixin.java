package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.GpuCubes;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderTypeFeatureRenderer.class)
public abstract class RenderTypeFeatureRendererGpuCubesMixin {

    @Redirect(method = "executeGroup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/StagedVertexBuffer;getExecuteInfo(Lnet/minecraft/client/renderer/StagedVertexBuffer$Draw;)Lnet/minecraft/client/renderer/StagedVertexBuffer$ExecuteInfo;"))
    private StagedVertexBuffer.ExecuteInfo txoptimizations$planGpuCubes(StagedVertexBuffer staged, StagedVertexBuffer.Draw draw) {
        return GpuCubes.executeInfo(staged, draw);
    }
}
