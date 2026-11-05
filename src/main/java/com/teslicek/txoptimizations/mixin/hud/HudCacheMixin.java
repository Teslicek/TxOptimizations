package com.teslicek.txoptimizations.mixin.hud;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.teslicek.txoptimizations.hud.HudCache;
import com.teslicek.txoptimizations.hud.HudLayerTimer;
import com.teslicek.txoptimizations.hud.HudLayers;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.spectator.SpectatorGui;
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

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractCameraOverlays(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void txoptimizations$cacheCameraOverlays(Hud hud, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        HudLayerTimer layer = HudCache.beginLayer(HudLayers.CAMERA_OVERLAYS);

        if (layer == HudCache.SKIPPED)
            return;

        original.call(hud, graphics, deltaTracker);
        HudCache.endLayer(layer);
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractCrosshair(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void txoptimizations$cacheCrosshair(Hud hud, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        HudLayerTimer layer = HudCache.beginLayer(HudLayers.CROSSHAIR);

        if (layer == HudCache.SKIPPED)
            return;

        original.call(hud, graphics, deltaTracker);
        HudCache.endLayer(layer);
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractEffects(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void txoptimizations$cacheEffects(Hud hud, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        HudLayerTimer layer = HudCache.beginLayer(HudLayers.EFFECTS);

        if (layer == HudCache.SKIPPED)
            return;

        original.call(hud, graphics, deltaTracker);
        HudCache.endLayer(layer);
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractBossOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void txoptimizations$cacheBossBar(Hud hud, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        HudLayerTimer layer = HudCache.beginLayer(HudLayers.BOSS_BAR);

        if (layer == HudCache.SKIPPED)
            return;

        original.call(hud, graphics, deltaTracker);
        HudCache.endLayer(layer);
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractSleepOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void txoptimizations$cacheSleep(Hud hud, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        HudLayerTimer layer = HudCache.beginLayer(HudLayers.SLEEP);

        if (layer == HudCache.SKIPPED)
            return;

        original.call(hud, graphics, deltaTracker);
        HudCache.endLayer(layer);
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractDemoOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void txoptimizations$cacheDemo(Hud hud, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        HudLayerTimer layer = HudCache.beginLayer(HudLayers.DEMO);

        if (layer == HudCache.SKIPPED)
            return;

        original.call(hud, graphics, deltaTracker);
        HudCache.endLayer(layer);
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void txoptimizations$cacheScoreboard(Hud hud, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        HudLayerTimer layer = HudCache.beginLayer(HudLayers.SCOREBOARD);

        if (layer == HudCache.SKIPPED)
            return;

        original.call(hud, graphics, deltaTracker);
        HudCache.endLayer(layer);
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractOverlayMessage(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void txoptimizations$cacheOverlayMessage(Hud hud, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        HudLayerTimer layer = HudCache.beginLayer(HudLayers.OVERLAY_MESSAGE);

        if (layer == HudCache.SKIPPED)
            return;

        original.call(hud, graphics, deltaTracker);
        HudCache.endLayer(layer);
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractTitle(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void txoptimizations$cacheTitle(Hud hud, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        HudLayerTimer layer = HudCache.beginLayer(HudLayers.TITLE);

        if (layer == HudCache.SKIPPED)
            return;

        original.call(hud, graphics, deltaTracker);
        HudCache.endLayer(layer);
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractChat(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void txoptimizations$cacheChat(Hud hud, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        HudLayerTimer layer = HudCache.beginLayer(HudLayers.CHAT);

        if (layer == HudCache.SKIPPED)
            return;

        original.call(hud, graphics, deltaTracker);
        HudCache.endLayer(layer);
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractTabList(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void txoptimizations$cacheTabList(Hud hud, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        HudLayerTimer layer = HudCache.beginLayer(HudLayers.TAB_LIST);

        if (layer == HudCache.SKIPPED)
            return;

        original.call(hud, graphics, deltaTracker);
        HudCache.endLayer(layer);
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractSubtitleOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Z)V"), require = 2)
    private void txoptimizations$cacheSubtitles(Hud hud, GuiGraphicsExtractor graphics, boolean deferred, Operation<Void> original) {
        HudLayerTimer layer = HudCache.beginLayer(HudLayers.SUBTITLES);

        if (layer == HudCache.SKIPPED)
            return;

        original.call(hud, graphics, deferred);
        HudCache.endLayer(layer);
    }

    @WrapOperation(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/spectator/SpectatorGui;extractHotbar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
    private void txoptimizations$cacheSpectatorHotbar(SpectatorGui spectator, GuiGraphicsExtractor graphics, Operation<Void> original) {
        HudLayerTimer layer = HudCache.beginLayer(HudLayers.HOTBAR);

        if (layer == HudCache.SKIPPED)
            return;

        original.call(spectator, graphics);
        HudCache.endLayer(layer);
    }

    @WrapOperation(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractItemHotbar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void txoptimizations$cacheItemHotbar(Hud hud, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        HudLayerTimer layer = HudCache.beginLayer(HudLayers.HOTBAR);

        if (layer == HudCache.SKIPPED)
            return;

        original.call(hud, graphics, deltaTracker);
        HudCache.endLayer(layer);
    }

    @WrapOperation(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractPlayerHealth(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
    private void txoptimizations$cachePlayerHealth(Hud hud, GuiGraphicsExtractor graphics, Operation<Void> original) {
        HudLayerTimer layer = HudCache.beginLayer(HudLayers.PLAYER_HEALTH);

        if (layer == HudCache.SKIPPED)
            return;

        original.call(hud, graphics);
        HudCache.endLayer(layer);
    }

    @WrapOperation(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractVehicleHealth(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
    private void txoptimizations$cacheVehicleHealth(Hud hud, GuiGraphicsExtractor graphics, Operation<Void> original) {
        HudLayerTimer layer = HudCache.beginLayer(HudLayers.VEHICLE_HEALTH);

        if (layer == HudCache.SKIPPED)
            return;

        original.call(hud, graphics);
        HudCache.endLayer(layer);
    }

    @WrapOperation(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void txoptimizations$cacheContextualBackground(ContextualBar bar, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        HudLayerTimer layer = HudCache.beginLayer(HudLayers.CONTEXTUAL_BACKDROP);

        if (layer == HudCache.SKIPPED)
            return;

        original.call(bar, graphics, deltaTracker);
        HudCache.endLayer(layer);
    }

    @WrapOperation(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"))
    private void txoptimizations$cacheExperienceLevel(GuiGraphicsExtractor graphics, Font font, int level, Operation<Void> original) {
        HudLayerTimer layer = HudCache.beginLayer(HudLayers.EXPERIENCE_LEVEL);

        if (layer == HudCache.SKIPPED)
            return;

        original.call(graphics, font, level);
        HudCache.endLayer(layer);
    }

    @WrapOperation(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void txoptimizations$cacheContextualBar(ContextualBar bar, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        HudLayerTimer layer = HudCache.beginLayer(HudLayers.CONTEXTUAL_BAR);

        if (layer == HudCache.SKIPPED)
            return;

        original.call(bar, graphics, deltaTracker);
        HudCache.endLayer(layer);
    }

    @WrapOperation(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractSelectedItemName(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
    private void txoptimizations$cacheSelectedItemName(Hud hud, GuiGraphicsExtractor graphics, Operation<Void> original) {
        HudLayerTimer layer = HudCache.beginLayer(HudLayers.SELECTED_ITEM);

        if (layer == HudCache.SKIPPED)
            return;

        original.call(hud, graphics);
        HudCache.endLayer(layer);
    }

    @WrapOperation(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/spectator/SpectatorGui;extractAction(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
    private void txoptimizations$cacheSpectatorAction(SpectatorGui spectator, GuiGraphicsExtractor graphics, Operation<Void> original) {
        HudLayerTimer layer = HudCache.beginLayer(HudLayers.SELECTED_ITEM);

        if (layer == HudCache.SKIPPED)
            return;

        original.call(spectator, graphics);
        HudCache.endLayer(layer);
    }
}
