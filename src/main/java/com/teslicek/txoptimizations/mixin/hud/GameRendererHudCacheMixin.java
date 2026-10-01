package com.teslicek.txoptimizations.mixin.hud;

import com.teslicek.txoptimizations.hud.HudCache;
import com.teslicek.txoptimizations.hud.HudDeltaTracker;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererHudCacheMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private boolean txoptimizations$screenOpen;

    @Unique
    private boolean txoptimizations$hudHidden;

    @Inject(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V"))
    private void txoptimizations$prepareHudCache(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        if (deltaTracker instanceof DeltaTracker.Timer timer)
            HudDeltaTracker.accumulate(timer, HudCache.currentPass());
        else
            HudDeltaTracker.disable();

        boolean screenOpen = this.minecraft.gui.screen() != null;
        boolean hudHidden  = this.minecraft.gui.hud.isHidden();

        if (screenOpen == this.txoptimizations$screenOpen && hudHidden == this.txoptimizations$hudHidden)
            return;

        this.txoptimizations$screenOpen = screenOpen;
        this.txoptimizations$hudHidden  = hudHidden;
        HudCache.markForCatchUp();
    }
}
