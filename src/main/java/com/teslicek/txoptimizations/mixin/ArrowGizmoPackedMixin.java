package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.PackedGizmoLines;
import net.minecraft.gizmos.ArrowGizmo;
import net.minecraft.gizmos.GizmoPrimitives;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ArrowGizmo.class)
public abstract class ArrowGizmoPackedMixin {

    @Unique
    private static final Quaternionf ROTATION = new Quaternionf();

    @Unique
    private static final Vector3f DIRECTION = new Vector3f();

    @Unique
    private static final float[] ROTATION_MATRIX = new float[9];

    @Shadow
    @Final
    private Vec3 start;

    @Shadow
    @Final
    private Vec3 end;

    @Shadow
    @Final
    private int color;

    @Shadow
    @Final
    private float width;

    @Overwrite
    public void emit(GizmoPrimitives primitives, float alphaMultiplier) {
        int color = ARGB.multiplyAlpha(this.color, alphaMultiplier);

        if (!(primitives instanceof PackedGizmoLines packed))
            throw new IllegalStateException("Gizmo primitives are not packed: " + primitives.getClass().getName());

        packed.txoptimizations$addLine(this.start.x, this.start.y, this.start.z, this.end.x, this.end.y, this.end.z, color, this.width);
        DIRECTION.set((float) (this.end.x + -this.start.x), (float) (this.end.y + -this.start.y), (float) (this.end.z + -this.start.z)).normalize();
        ROTATION.rotationTo(1.0F, 0.0F, 0.0F, DIRECTION.x, DIRECTION.y, DIRECTION.z);

        float len = (float) Mth.clamp(this.end.distanceTo(this.start) * 0.1F, 0.1F, 1.0);

        txoptimizations$prepareRotationMatrix();

        txoptimizations$tip(packed, -len, len, 0.0F, color);
        txoptimizations$tip(packed, -len, 0.0F, len, color);
        txoptimizations$tip(packed, -len, -len, 0.0F, color);
        txoptimizations$tip(packed, -len, 0.0F, -len, color);
    }

    @Unique
    private static void txoptimizations$prepareRotationMatrix() {
        float xx = ROTATION.x * ROTATION.x;
        float yy = ROTATION.y * ROTATION.y;
        float zz = ROTATION.z * ROTATION.z;
        float ww = ROTATION.w * ROTATION.w;
        float xy = ROTATION.x * ROTATION.y;
        float xz = ROTATION.x * ROTATION.z;
        float yz = ROTATION.y * ROTATION.z;
        float xw = ROTATION.x * ROTATION.w;
        float zw = ROTATION.z * ROTATION.w;
        float yw = ROTATION.y * ROTATION.w;
        float k  = 1.0F / (xx + yy + zz + ww);

        ROTATION_MATRIX[0] = (xx - yy - zz + ww) * k;
        ROTATION_MATRIX[1] = 2.0F * (xy - zw) * k;
        ROTATION_MATRIX[2] = 2.0F * (xz + yw) * k;
        ROTATION_MATRIX[3] = 2.0F * (xy + zw) * k;
        ROTATION_MATRIX[4] = (yy - xx - zz + ww) * k;
        ROTATION_MATRIX[5] = 2.0F * (yz - xw) * k;
        ROTATION_MATRIX[6] = 2.0F * (xz - yw) * k;
        ROTATION_MATRIX[7] = 2.0F * (yz + xw) * k;
        ROTATION_MATRIX[8] = (zz - xx - yy + ww) * k;
    }

    @Unique
    private void txoptimizations$tip(PackedGizmoLines packed, float x, float y, float z, int color) {
        float[] m    = ROTATION_MATRIX;
        float   tipX = org.joml.Math.fma(m[0], x, org.joml.Math.fma(m[1], y, m[2] * z));
        float   tipY = org.joml.Math.fma(m[3], x, org.joml.Math.fma(m[4], y, m[5] * z));
        float   tipZ = org.joml.Math.fma(m[6], x, org.joml.Math.fma(m[7], y, m[8] * z));

        packed.txoptimizations$addLine(this.end.x + tipX, this.end.y + tipY, this.end.z + tipZ, this.end.x, this.end.y, this.end.z, color, this.width);
    }
}
