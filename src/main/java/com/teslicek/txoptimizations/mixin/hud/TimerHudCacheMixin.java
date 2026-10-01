package com.teslicek.txoptimizations.mixin.hud;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.teslicek.txoptimizations.hud.HudDeltaTracker;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DeltaTracker.Timer.class)
public abstract class TimerHudCacheMixin {

    @ModifyReturnValue(method = "getRealtimeDeltaTicks", at = @At("RETURN"))
    private float txoptimizations$cycleRealtimeDelta(float delta) {
        return HudDeltaTracker.realtimeDelta(delta);
    }

    @ModifyReturnValue(method = "getGameTimeDeltaTicks", at = @At("RETURN"))
    private float txoptimizations$cycleGameTimeDelta(float delta) {
        return HudDeltaTracker.gameTimeDelta(delta);
    }
}
