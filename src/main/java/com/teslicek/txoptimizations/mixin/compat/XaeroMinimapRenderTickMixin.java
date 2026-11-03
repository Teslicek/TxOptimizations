package com.teslicek.txoptimizations.mixin.compat;

import com.teslicek.txoptimizations.RenderWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "xaero.common.events.ClientEvents", remap = false)
public abstract class XaeroMinimapRenderTickMixin {

    @Inject(method = "handleRenderTickStart", at = @At("HEAD"))
    private void txoptimizations$awaitRenderBeforeRenderTick(CallbackInfo ci) {
        RenderWorker.awaitFromMain();
    }
}
