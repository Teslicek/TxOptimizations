package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.systems.ScissorState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ScissorState.class)
public abstract class ScissorStateEqualityMixin {

    @Shadow
    private boolean enabled;

    @Shadow
    private int x;

    @Shadow
    private int y;

    @Shadow
    private int width;

    @Shadow
    private int height;

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (!(other instanceof ScissorStateEqualityMixin that))
            return false;

        return this.enabled == that.enabled && this.x == that.x && this.y == that.y && this.width == that.width && this.height == that.height;
    }

    @Override
    public int hashCode() {
        int hash = Boolean.hashCode(this.enabled);
        hash = 31 * hash + this.x;
        hash = 31 * hash + this.y;
        hash = 31 * hash + this.width;

        return 31 * hash + this.height;
    }
}
