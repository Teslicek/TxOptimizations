package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.GpuCubeDraw;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer$Group")
public abstract class RenderTypeGroupGpuCubesMixin {

    @Inject(method = "getOrAddDraw", at = @At("RETURN"))
    private void txoptimizations$tagRenderType(CallbackInfoReturnable<StagedVertexBuffer.Draw> cir, @Local PreparedRenderType prepared) {
        ((GpuCubeDraw) cir.getReturnValue()).txoptimizations$setPrepared(prepared);
    }
}
