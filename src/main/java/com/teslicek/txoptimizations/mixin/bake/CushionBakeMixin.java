package com.teslicek.txoptimizations.mixin.bake;

import com.teslicek.txoptimizations.bake.Bakeable;
import com.teslicek.txoptimizations.bake.Cushions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Cushion.class)
public abstract class CushionBakeMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void txoptimizations$markBakeable(EntityType<Cushion> type, Level level, CallbackInfo ci) {
        ((Bakeable) this).txoptimizations$setBakeSupported(type == EntityTypes.CUSHION);
    }

    @Inject(method = "setPos(DDD)V", at = @At("HEAD"))
    private void txoptimizations$untrackOldPosition(double x, double y, double z, CallbackInfo ci) {
        Cushions.untrack((Entity) (Object) this);
    }

    @Inject(method = "setPos(DDD)V", at = @At("TAIL"))
    private void txoptimizations$trackNewPosition(double x, double y, double z, CallbackInfo ci) {
        Cushions.track((Entity) (Object) this);
    }

    @Inject(method = "setColor", at = @At("TAIL"))
    private void txoptimizations$trackColor(DyeColor color, CallbackInfo ci) {
        Cushions.track((Entity) (Object) this);
    }
}
