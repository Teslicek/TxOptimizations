package com.teslicek.txoptimizations.mixin.hud;

import com.teslicek.txoptimizations.hud.HudCache;
import com.teslicek.txoptimizations.hud.HudLayers;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftHudCacheMixin {

    @Inject(method = "onGameLoadFinished", at = @At("TAIL"))
    private void txoptimizations$registerHudLayers(CallbackInfo ci) {
        HudCache.registerLayers(HudLayers.ORDER);
    }

    @Inject(method = "setLevel", at = @At("TAIL"))
    private void txoptimizations$resetHudCache(CallbackInfo ci) {
        HudCache.reset();
    }
}
