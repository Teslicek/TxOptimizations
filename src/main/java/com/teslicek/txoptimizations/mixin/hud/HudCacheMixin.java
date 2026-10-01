package com.teslicek.txoptimizations.mixin.hud;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.teslicek.txoptimizations.hud.HudCache;
import com.teslicek.txoptimizations.hud.HudLayerTimer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Hud.class, priority = 5000)
public abstract class HudCacheMixin {

    @WrapMethod(method = "extractRenderState")
    private void txoptimizations$cacheHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        HudCache.extract(graphics, () -> original.call(graphics, deltaTracker));
    }

    @Inject(method = "extractSubtitleOverlay", at = @At("HEAD"))
    private void txoptimizations$drawSubtitlesInPlace(GuiGraphicsExtractor graphics, boolean deferred, CallbackInfo ci, @Local(argsOnly = true) LocalBooleanRef defer) {
        defer.set(false);
    }

    @WrapOperation(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void txoptimizations$cacheContextualBar(ContextualBar bar, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        if (!HudCache.isRendering()) {
            original.call(bar, graphics, deltaTracker);

            return;
        }

        HudLayerTimer layer = HudCache.layer(VanillaHudElements.EXPERIENCE_LEVEL);

        if (!layer.shouldRender(HudCache.currentPass()))
            return;

        HudCache.begin(layer);
        original.call(bar, graphics, deltaTracker);
        HudCache.end(layer);
    }
}
