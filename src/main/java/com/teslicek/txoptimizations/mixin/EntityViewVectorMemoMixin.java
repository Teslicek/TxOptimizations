package com.teslicek.txoptimizations.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Entity.class, priority = 999)
public abstract class EntityViewVectorMemoMixin {

    @Unique
    private Vec3 txoptimizations$viewVector;

    @Unique
    private int txoptimizations$viewXRot;

    @Unique
    private int txoptimizations$viewYRot;

    @Shadow
    public abstract float getViewXRot(float partialTicks);

    @Shadow
    public abstract float getViewYRot(float partialTicks);

    @Overwrite
    public final Vec3 getViewVector(float partialTicks) {
        float xRot  = this.getViewXRot(partialTicks);
        float yRot  = this.getViewYRot(partialTicks);
        int   xBits = Float.floatToRawIntBits(xRot);
        int   yBits = Float.floatToRawIntBits(yRot);
        Vec3  last  = this.txoptimizations$viewVector;

        if (last != null && xBits == this.txoptimizations$viewXRot && yBits == this.txoptimizations$viewYRot)
            return last;

        Vec3 vector = Entity.calculateViewVector(xRot, yRot);

        this.txoptimizations$viewVector = vector;
        this.txoptimizations$viewXRot   = xBits;
        this.txoptimizations$viewYRot   = yBits;

        return vector;
    }
}
