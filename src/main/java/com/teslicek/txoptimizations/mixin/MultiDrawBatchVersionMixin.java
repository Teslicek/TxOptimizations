package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.BatchVersion;
import net.caffeinemc.mods.sodium.client.gpu.device.batch.MultiDrawBatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MultiDrawBatch.class, remap = false)
public abstract class MultiDrawBatchVersionMixin implements BatchVersion {

    @Unique
    private int txoptimizations$version;

    @Inject(method = "clear", at = @At("HEAD"))
    private void txoptimizations$bumpVersion(CallbackInfo ci) {
        this.txoptimizations$version ++;
    }

    @Override
    public int txoptimizations$getVersion() {
        return this.txoptimizations$version;
    }
}
