package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.RenderWorker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameRenderer.class, LevelRenderer.class})
public abstract class RendererRenderJobWaitMixin {

    @Inject(method = {"resize", "close"}, at = @At("HEAD"), require = 2)
    private void txoptimizations$waitBeforeRebuild(CallbackInfo ci) {
        RenderWorker.awaitFromMain();
    }
}
