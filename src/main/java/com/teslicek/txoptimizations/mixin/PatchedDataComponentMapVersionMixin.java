package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ComponentVersion;
import net.minecraft.core.component.PatchedDataComponentMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PatchedDataComponentMap.class)
public abstract class PatchedDataComponentMapVersionMixin implements ComponentVersion {

    @Unique
    private int txoptimizations$version;

    @Inject(method = "ensureMapOwnership", at = @At("HEAD"))
    private void txoptimizations$bumpVersion(CallbackInfo ci) {
        this.txoptimizations$version ++;
    }

    @Override
    public int txoptimizations$version() {
        return this.txoptimizations$version;
    }
}
