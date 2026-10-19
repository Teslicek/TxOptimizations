package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SectionSubset;
import net.minecraft.client.RotatingSectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RotatingSectionStorage.class)
public abstract class RotatingSectionStorageChangeMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$markCreated(int radius, int minY, int maxY, RotatingSectionStorage.ValueCreator<?> valueCreator, CallbackInfo ci) {
        SectionSubset.DIRTY.markChange();
    }
}
