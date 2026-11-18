package com.teslicek.txoptimizations.mixin.compat;

import com.teslicek.txoptimizations.RenderWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "xaero.lib.client.event.ClientEvents", remap = false)
public abstract class XaeroLibEndFrameMixin {

    @Inject(method = "onPostRenderTick", at = @At("HEAD"))
    private void txoptimizations$awaitRenderBeforeEndFrame(CallbackInfo ci) {
        RenderWorker.awaitFromMain();
    }
}
