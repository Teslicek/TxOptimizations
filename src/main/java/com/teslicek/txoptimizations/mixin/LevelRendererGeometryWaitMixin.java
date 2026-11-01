package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.RenderWorker;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererGeometryWaitMixin {

    @Inject(method = {"invalidateCompiledGeometry", "resetLevelRenderData"}, at = @At("HEAD"), require = 2)
    private void txoptimizations$waitBeforeInvalidating(CallbackInfo ci) {
        RenderWorker.awaitFromMain();
    }
}
