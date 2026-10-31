package com.teslicek.txoptimizations.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityRenderDistanceMixin {

    @Shadow
    private static double viewScale;

    @Shadow
    private AABB bb;

    @Unique
    private AABB txoptimizations$limitBox;

    @Unique
    private double txoptimizations$limitScale;

    @Unique
    private double txoptimizations$limit;

    @Overwrite
    public boolean shouldRenderAtSqrDistance(double distance) {
        AABB box = this.bb;

        if (box != this.txoptimizations$limitBox || viewScale != this.txoptimizations$limitScale) {
            double size = box.getSize();

            if (Double.isNaN(size))
                size = 1.0;

            size *= 64.0 * viewScale;

            this.txoptimizations$limit      = size * size;
            this.txoptimizations$limitBox   = box;
            this.txoptimizations$limitScale = viewScale;
        }

        return distance < this.txoptimizations$limit;
    }
}
