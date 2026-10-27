package com.teslicek.txoptimizations;

import net.minecraft.gizmos.GizmoPrimitives;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class GizmoShapes {

    private static final int         X_CORNER_TINT   = -34953;
    private static final int         Y_CORNER_TINT   = -8913033;
    private static final int         Z_CORNER_TINT   = -8947713;
    private static final int         TIP_SLOTS       = 1024;
    private static final int         TIP_VALUES      = 12;
    private static final Quaternionf ROTATION        = new Quaternionf();
    private static final Vector3f    DIRECTION       = new Vector3f();
    private static final float[]     ROTATION_MATRIX = new float[9];
    private static final long[]      TIP_KEYS        = new long[TIP_SLOTS * 3];
    private static final boolean[]   TIP_FILLED      = new boolean[TIP_SLOTS];
    private static final float[]     TIPS            = new float[TIP_SLOTS * TIP_VALUES];

    private GizmoShapes() {
    }

    public static void emitPoint(GizmoPrimitives primitives, double x, double y, double z, int color, float size) {
        if (primitives instanceof PackedGizmoPrimitives packed) {
            packed.txoptimizations$addPoint(x, y, z, color, size);
            return;
        }

        primitives.addPoint(new Vec3(x, y, z), color, size);
    }

    public static void emitCuboid(GizmoPrimitives primitives, double x0, double y0, double z0, double x1, double y1, double z1, GizmoStyle style, boolean coloredCorner, float alphaMultiplier) {
        if (primitives instanceof PackedGizmoPrimitives packed && !style.hasFill()) {
            if (!style.hasStroke() || x0 == x1 && y0 == y1 && z0 == z1)
                return;

            int color = style.multipliedStroke(alphaMultiplier);

            packed.txoptimizations$addBox(x0, y0, z0, x1, y1, z1, color, coloredCorner ? ARGB.multiply(color, X_CORNER_TINT) : color, coloredCorner ? ARGB.multiply(color, Y_CORNER_TINT) : color, coloredCorner ? ARGB.multiply(color, Z_CORNER_TINT) : color, style.strokeWidth());

            return;
        }

        Vec3 a = new Vec3(x0, y0, z0);
        Vec3 b = new Vec3(x1, y0, z0);
        Vec3 c = new Vec3(x1, y1, z0);
        Vec3 d = new Vec3(x0, y1, z0);
        Vec3 e = new Vec3(x0, y0, z1);
        Vec3 f = new Vec3(x1, y0, z1);
        Vec3 g = new Vec3(x1, y1, z1);
        Vec3 h = new Vec3(x0, y1, z1);

        if (style.hasFill()) {
            int color = style.multipliedFill(alphaMultiplier);

            primitives.addQuad(b, c, g, f, color);
            primitives.addQuad(a, e, h, d, color);
            primitives.addQuad(a, d, c, b, color);
            primitives.addQuad(e, f, g, h, color);
            primitives.addQuad(d, h, g, c, color);
            primitives.addQuad(a, b, f, e, color);
        }

        if (!style.hasStroke())
            return;

        int   color = style.multipliedStroke(alphaMultiplier);
        float width = style.strokeWidth();

        primitives.addLine(a, b, coloredCorner ? ARGB.multiply(color, X_CORNER_TINT) : color, width);
        primitives.addLine(a, d, coloredCorner ? ARGB.multiply(color, Y_CORNER_TINT) : color, width);
        primitives.addLine(a, e, coloredCorner ? ARGB.multiply(color, Z_CORNER_TINT) : color, width);
        primitives.addLine(b, c, color, width);
        primitives.addLine(c, d, color, width);
        primitives.addLine(d, h, color, width);
        primitives.addLine(h, e, color, width);
        primitives.addLine(e, f, color, width);
        primitives.addLine(f, b, color, width);
        primitives.addLine(h, g, color, width);
        primitives.addLine(f, g, color, width);
        primitives.addLine(c, g, color, width);
    }

    public static void emitArrow(GizmoPrimitives primitives, double startX, double startY, double startZ, double endX, double endY, double endZ, int argb, float width, float alphaMultiplier) {
        int color = ARGB.multiplyAlpha(argb, alphaMultiplier);

        if (!(primitives instanceof PackedGizmoPrimitives packed))
            throw new IllegalStateException("Gizmo primitives are not packed: " + primitives.getClass().getName());

        packed.txoptimizations$addLine(startX, startY, startZ, endX, endY, endZ, color, width);

        double distanceX = startX - endX;
        double distanceY = startY - endY;
        double distanceZ = startZ - endZ;
        long   keyX      = Double.doubleToRawLongBits(distanceX);
        long   keyY      = Double.doubleToRawLongBits(distanceY);
        long   keyZ      = Double.doubleToRawLongBits(distanceZ);
        long   mixed     = (keyX * 0x9E3779B97F4A7C15L + keyY) * 0x9E3779B97F4A7C15L + keyZ;
        int    slot      = (int) (mixed * 0x9E3779B97F4A7C15L >>> 54);
        int    keyBase   = slot * 3;
        int    tipBase   = slot * TIP_VALUES;

        if (!TIP_FILLED[slot] || TIP_KEYS[keyBase] != keyX || TIP_KEYS[keyBase + 1] != keyY || TIP_KEYS[keyBase + 2] != keyZ) {
            DIRECTION.set((float) (endX + -startX), (float) (endY + -startY), (float) (endZ + -startZ)).normalize();
            ROTATION.rotationTo(1.0F, 0.0F, 0.0F, DIRECTION.x, DIRECTION.y, DIRECTION.z);

            float len = (float) Mth.clamp(Math.sqrt(distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ) * 0.1F, 0.1F, 1.0);

            prepareRotationMatrix();
            rotateTip(tipBase, -len, len, 0.0F);
            rotateTip(tipBase + 3, -len, 0.0F, len);
            rotateTip(tipBase + 6, -len, -len, 0.0F);
            rotateTip(tipBase + 9, -len, 0.0F, -len);

            TIP_KEYS[keyBase]     = keyX;
            TIP_KEYS[keyBase + 1] = keyY;
            TIP_KEYS[keyBase + 2] = keyZ;
            TIP_FILLED[slot]      = true;
        }

        for (int tip = tipBase; tip < tipBase + TIP_VALUES; tip += 3)
            packed.txoptimizations$addLine(endX + TIPS[tip], endY + TIPS[tip + 1], endZ + TIPS[tip + 2], endX, endY, endZ, color, width);
    }

    private static void prepareRotationMatrix() {
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

    private static void rotateTip(int base, float x, float y, float z) {
        float[] m = ROTATION_MATRIX;

        TIPS[base]     = org.joml.Math.fma(m[0], x, org.joml.Math.fma(m[1], y, m[2] * z));
        TIPS[base + 1] = org.joml.Math.fma(m[3], x, org.joml.Math.fma(m[4], y, m[5] * z));
        TIPS[base + 2] = org.joml.Math.fma(m[6], x, org.joml.Math.fma(m[7], y, m[8] * z));
    }
}
