package com.teslicek.txoptimizations.mixin.bake;

import com.teslicek.txoptimizations.bake.Bakeable;
import com.teslicek.txoptimizations.bake.Cushions;
import com.teslicek.txoptimizations.bake.RenderMode;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Cushion.class)
public abstract class CushionBakeMixin extends BlockAttachedEntity implements Bakeable {

    @Unique
    private RenderMode txoptimizations$renderMode = RenderMode.ENTITY;

    @Unique
    private RenderMode txoptimizations$pendingMode = RenderMode.TERRAIN;

    @Unique
    private boolean txoptimizations$bakeSupported;

    protected CushionBakeMixin(EntityType<? extends BlockAttachedEntity> type, Level level) {
        super(type, level);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void txoptimizations$markBakeable(EntityType<Cushion> type, Level level, CallbackInfo ci) {
        this.txoptimizations$bakeSupported = type == EntityTypes.CUSHION;
    }

    @Inject(method = "setPos(DDD)V", at = @At("HEAD"))
    private void txoptimizations$untrackOldPosition(double x, double y, double z, CallbackInfo ci) {
        Cushions.untrack(this);
    }

    @Inject(method = "setPos(DDD)V", at = @At("TAIL"))
    private void txoptimizations$trackNewPosition(double x, double y, double z, CallbackInfo ci) {
        Cushions.track(this);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);

        if (this.txoptimizations$bakeSupported)
            Cushions.track(this);
    }

    @Override
    public boolean txoptimizations$isBakeSupported() {
        return this.txoptimizations$bakeSupported;
    }

    @Override
    public RenderMode txoptimizations$getRenderMode() {
        return this.txoptimizations$renderMode;
    }

    @Override
    public void txoptimizations$setRenderMode(RenderMode mode) {
        this.txoptimizations$renderMode  = mode;
        this.txoptimizations$pendingMode = mode;
    }

    @Override
    public RenderMode txoptimizations$getPendingMode() {
        return this.txoptimizations$pendingMode;
    }

    @Override
    public void txoptimizations$setPendingMode(RenderMode mode) {
        this.txoptimizations$pendingMode = mode;
    }
}
