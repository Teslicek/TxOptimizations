package com.teslicek.txoptimizations.mixin;

import net.minecraft.core.Cursor3D;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Cursor3D.class)
public abstract class CursorCounterMixin {

    @Shadow
    @Final
    private int width;

    @Shadow
    @Final
    private int height;

    @Shadow
    @Final
    private int depth;

    @Shadow
    @Final
    private int end;

    @Shadow
    private int index;

    @Shadow
    private int x;

    @Shadow
    private int y;

    @Shadow
    private int z;

    @Overwrite
    public boolean advance() {
        if (this.index == this.end)
            return false;

        if (this.width <= 0 || this.height <= 0 || this.depth <= 0) {
            int slice = this.index / this.width;

            this.x = this.index % this.width;
            this.y = slice % this.height;
            this.z = slice / this.height;
        } else if (this.index > 0 && ++ this.x == this.width) {
            this.x = 0;

            if (++ this.y == this.height) {
                this.y = 0;
                this.z ++;
            }
        }

        this.index ++;

        return true;
    }
}
