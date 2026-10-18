package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.teslicek.txoptimizations.SectionSubset;
import net.minecraft.client.RotatingSectionStorage;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RotatingSectionStorage.class)
public abstract class RotatingSectionStorageChangeMixin {

    @Shadow
    private SectionPos centerSectionPos;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$markCreated(int radius, int minY, int maxY, RotatingSectionStorage.ValueCreator<?> valueCreator, CallbackInfo ci) {
        SectionSubset.DIRTY.markChange();
    }

    @Inject(method = "repositionCenter", at = @At("HEAD"))
    private void txoptimizations$beginReposition(SectionPos newCenterSectionPos, CallbackInfoReturnable<Boolean> cir) {
        if (!newCenterSectionPos.equals(this.centerSectionPos))
            SectionSubset.DIRTY.beginChange();
    }

    @ModifyReturnValue(method = "repositionCenter", at = @At("RETURN"))
    private boolean txoptimizations$endReposition(boolean moved) {
        if (moved)
            SectionSubset.DIRTY.endChange();

        return moved;
    }
}
