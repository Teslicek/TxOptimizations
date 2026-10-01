package com.teslicek.txoptimizations.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "me.flashyreese.mods.sodiumextra.client.FrameCounter")
public abstract class SodiumExtraFrameCounterMixin {

    @Inject(method = "onFrame", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$skipSodiumExtraFrameStats(CallbackInfo ci) {
        ci.cancel();
    }
}
