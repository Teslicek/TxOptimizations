package com.teslicek.txoptimizations.mixin.bake;

import com.teslicek.txoptimizations.bake.Bakeable;
import com.teslicek.txoptimizations.bake.Cushions;
import com.teslicek.txoptimizations.bake.RenderMode;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityBakeStateMixin implements Bakeable {

    @Unique
    private RenderMode txoptimizations$renderMode = RenderMode.ENTITY;

    @Unique
    private RenderMode txoptimizations$pendingMode = RenderMode.TERRAIN;

    @Unique
    private boolean txoptimizations$bakeSupported;

    @Inject(method = "onSyncedDataUpdated(Lnet/minecraft/network/syncher/EntityDataAccessor;)V", at = @At("TAIL"))
    private void txoptimizations$retrackOnDataUpdate(CallbackInfo ci) {
        if (!this.txoptimizations$bakeSupported)
            return;

        Cushions.track((Entity) (Object) this);
    }

    @Override
    public boolean txoptimizations$isBakeSupported() {
        return this.txoptimizations$bakeSupported;
    }

    @Override
    public void txoptimizations$setBakeSupported(boolean supported) {
        this.txoptimizations$bakeSupported = supported;
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
