package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.EntityVisibilityMemo;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityVisibilityMemoMixin implements EntityVisibilityMemo {

    @Unique
    private long txoptimizations$visibilityFrame = -1L;

    @Unique
    private float txoptimizations$visibilityPartialTick;

    @Unique
    private boolean txoptimizations$visible;

    @Override
    public boolean txoptimizations$hasVisibility(long frame, float partialTick) {
        return this.txoptimizations$visibilityFrame == frame && this.txoptimizations$visibilityPartialTick == partialTick;
    }

    @Override
    public boolean txoptimizations$isVisible() {
        return this.txoptimizations$visible;
    }

    @Override
    public void txoptimizations$setVisibility(long frame, float partialTick, boolean visible) {
        this.txoptimizations$visibilityFrame       = frame;
        this.txoptimizations$visibilityPartialTick = partialTick;
        this.txoptimizations$visible               = visible;
    }
}
