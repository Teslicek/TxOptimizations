package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SharedFlags;
import net.minecraft.network.syncher.SyncedDataHolder;
import net.minecraft.network.syncher.SynchedEntityData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SynchedEntityData.class)
public abstract class SynchedEntityDataSharedFlagsMixin {

    @Shadow
    @Final
    private SyncedDataHolder entity;

    @Redirect(method = {"set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;Z)V", "assignValue"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/network/syncher/SynchedEntityData$DataItem;setValue(Ljava/lang/Object;)V"))
    private <T> void txoptimizations$storeValue(SynchedEntityData.DataItem<T> dataItem, T value) {
        dataItem.setValue(value);

        if (this.entity instanceof SharedFlags sharedFlags)
            sharedFlags.txoptimizations$storeSyncedValue(dataItem.getAccessor(), value);
    }
}
