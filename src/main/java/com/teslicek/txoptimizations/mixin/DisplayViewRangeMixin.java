package com.teslicek.txoptimizations.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Display.class)
public abstract class DisplayViewRangeMixin {

    @Shadow
    @Final
    private static EntityDataAccessor<Float> DATA_VIEW_RANGE_ID;

    @Unique
    private float txoptimizations$viewRange;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$initViewRange(EntityType<?> type, Level level, CallbackInfo ci) {
        this.txoptimizations$storeViewRange();
    }

    @Inject(method = "onSyncedDataUpdated(Lnet/minecraft/network/syncher/EntityDataAccessor;)V", at = @At("HEAD"))
    private void txoptimizations$updateViewRange(EntityDataAccessor<?> accessor, CallbackInfo ci) {
        if (DATA_VIEW_RANGE_ID.equals(accessor))
            this.txoptimizations$storeViewRange();
    }

    @Redirect(method = "shouldRenderAtSqrDistance", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Display;getViewRange()F"))
    private float txoptimizations$storedViewRange(Display display) {
        return this.txoptimizations$viewRange;
    }

    @Unique
    private void txoptimizations$storeViewRange() {
        this.txoptimizations$viewRange = ((Entity) (Object) this).getEntityData().get(DATA_VIEW_RANGE_ID);
    }
}
