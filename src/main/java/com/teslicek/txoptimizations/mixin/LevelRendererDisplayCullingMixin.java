package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teslicek.txoptimizations.DisplayCulling;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererDisplayCullingMixin {

    @Inject(method = "submitFeatures", at = @At("HEAD"))
    private void txoptimizations$beginDisplayCulling(LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector, boolean renderOutline, CallbackInfo ci) {
        DisplayCulling.begin(RenderSystem.getModelViewStack());
    }

    @Inject(method = "submitFeatures", at = @At("RETURN"))
    private void txoptimizations$endDisplayCulling(LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector, boolean renderOutline, CallbackInfo ci) {
        DisplayCulling.end();
    }
}
