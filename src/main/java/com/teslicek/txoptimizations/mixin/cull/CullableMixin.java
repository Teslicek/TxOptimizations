package com.teslicek.txoptimizations.mixin.cull;

import com.teslicek.txoptimizations.cull.Cullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({Entity.class, BlockEntity.class})
public abstract class CullableMixin implements Cullable {

    @Unique
    private boolean txoptimizations$culled;

    @Unique
    private long txoptimizations$visibleUntil;

    @Unique
    private boolean txoptimizations$outOfCamera;

    @Override
    public boolean txoptimizations$isCulled() {
        return this.txoptimizations$culled;
    }

    @Override
    public void txoptimizations$setCulled(boolean culled) {
        this.txoptimizations$culled = culled;
    }

    @Override
    public long txoptimizations$getVisibleUntil() {
        return this.txoptimizations$visibleUntil;
    }

    @Override
    public void txoptimizations$setVisibleUntil(long visibleUntil) {
        this.txoptimizations$visibleUntil = visibleUntil;
    }

    @Override
    public boolean txoptimizations$isOutOfCamera() {
        return this.txoptimizations$outOfCamera;
    }

    @Override
    public void txoptimizations$setOutOfCamera(boolean outOfCamera) {
        this.txoptimizations$outOfCamera = outOfCamera;
    }
}
