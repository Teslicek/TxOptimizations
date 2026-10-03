package com.teslicek.txoptimizations.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation.Estimator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Estimator.class, remap = false)
public abstract class EstimatorIdleUpdateMixin {

    @Unique
    private boolean txoptimizations$hasNewData;

    @Inject(method = "addData", at = @At("HEAD"))
    private void txoptimizations$markNewData(CallbackInfo ci) {
        this.txoptimizations$hasNewData = true;
    }

    @Inject(method = "updateModels", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$skipIdleUpdate(CallbackInfo ci) {
        if (!this.txoptimizations$hasNewData) {
            ci.cancel();

            return;
        }

        this.txoptimizations$hasNewData = false;
    }
}
