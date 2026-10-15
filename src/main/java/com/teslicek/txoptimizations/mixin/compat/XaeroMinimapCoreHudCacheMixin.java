package com.teslicek.txoptimizations.mixin.compat;

import com.teslicek.txoptimizations.hud.HudCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "xaero.common.core.XaeroMinimapCore", remap = false)
public abstract class XaeroMinimapCoreHudCacheMixin {

    @Inject(method = {"beforeIngameGuiRender", "afterIngameGuiRender"}, at = @At("HEAD"), cancellable = true)
    private static void txoptimizations$skipDuringHudCacheFlush(CallbackInfo ci) {
        if (HudCache.isFlushing())
            ci.cancel();
    }
}
