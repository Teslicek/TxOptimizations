package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Options;

public final class ModelRotation {

    private static final float                HALF                   = 0.5F;
    private static final int                  ROTATION_PROPERTIES    = Matrix4fc.PROPERTY_AFFINE | Matrix4fc.PROPERTY_ORTHONORMAL;
    private static final int                  ROTATED_PROPERTIES     = ~(Matrix4fc.PROPERTY_PERSPECTIVE | Matrix4fc.PROPERTY_IDENTITY | Matrix4fc.PROPERTY_TRANSLATION);
    private static final int                  TRANSLATION_PROPERTIES = Matrix4fc.PROPERTY_AFFINE | Matrix4fc.PROPERTY_TRANSLATION | Matrix4fc.PROPERTY_ORTHONORMAL;
    private static final int                  TRANSLATED_PROPERTIES  = ~(Matrix4fc.PROPERTY_PERSPECTIVE | Matrix4fc.PROPERTY_IDENTITY);
    private static final int                  ROTATION_VALUES        = 9;
    private static final int                  CACHE_SLOTS            = 1024;
    private static final int[]                CACHED_X               = new int[CACHE_SLOTS];
    private static final int[]                CACHED_Y               = new int[CACHE_SLOTS];
    private static final int[]                CACHED_Z               = new int[CACHE_SLOTS];
    private static final boolean[]            FILLED                 = new boolean[CACHE_SLOTS];
    private static final float[]              ROTATIONS              = new float[CACHE_SLOTS * ROTATION_VALUES];
    private static final ThreadLocal<float[]> SCRATCH                = ThreadLocal.withInitial(() -> new float[ROTATION_VALUES]);

    private ModelRotation() {
    }

    public static void rotateZYX(PoseStack.Pose pose, float angleZ, float angleY, float angleX) {
        Matrix4f matrix = pose.pose();

        rotateZYX(pose, pose, matrix.m30(), matrix.m31(), matrix.m32(), matrix.m33(), matrix.properties(), angleZ, angleY, angleX);
    }

    public static void pushTranslatedRotated(PoseStack stack, float x, float y, float z, float angleZ, float angleY, float angleX) {
        List<PoseStack.Pose> poses  = stack.poses;
        PoseStack.Pose       parent = poses.get(stack.lastIndex);

        stack.lastIndex ++;

        if (stack.lastIndex >= poses.size())
            poses.add(new PoseStack.Pose());

        PoseStack.Pose child = poses.get(stack.lastIndex);

        if (angleX == 0.0F && angleY == 0.0F && angleZ == 0.0F) {
            child.set(parent);
            child.translate(x, y, z);

            return;
        }

        Matrix4f matrix     = parent.pose();
        int      properties = matrix.properties();

        if ((properties & Matrix4fc.PROPERTY_IDENTITY) != 0)
            rotateZYX(child, parent, x, y, z, matrix.m33(), TRANSLATION_PROPERTIES, angleZ, angleY, angleX);
        else
            rotateZYX(child, parent, Math.fma(matrix.m00(), x, Math.fma(matrix.m10(), y, Math.fma(matrix.m20(), z, matrix.m30()))), Math.fma(matrix.m01(), x, Math.fma(matrix.m11(), y, Math.fma(matrix.m21(), z, matrix.m31()))), Math.fma(matrix.m02(), x, Math.fma(matrix.m12(), y, Math.fma(matrix.m22(), z, matrix.m32()))), Math.fma(matrix.m03(), x, Math.fma(matrix.m13(), y, Math.fma(matrix.m23(), z, matrix.m33()))), properties & TRANSLATED_PROPERTIES, angleZ, angleY, angleX);

        child.trustedNormals = parent.trustedNormals;
    }

    private static void rotateZYX(PoseStack.Pose target, PoseStack.Pose source, float m30, float m31, float m32, float m33, int properties, float angleZ, float angleY, float angleX) {
        float[] rotation;
        int     base;

        if (RenderThreadStack.isOwner()) {
            rotation = ROTATIONS;
            base     = rotationSlot(angleZ, angleY, angleX) * ROTATION_VALUES;
        } else {
            rotation = SCRATCH.get();
            base     = 0;
            fillRotation(rotation, base, angleZ, angleY, angleX);
        }

        float rm00 = rotation[base];
        float rm01 = rotation[base + 1];
        float rm02 = rotation[base + 2];
        float rm10 = rotation[base + 3];
        float rm11 = rotation[base + 4];
        float rm12 = rotation[base + 5];
        float rm20 = rotation[base + 6];
        float rm21 = rotation[base + 7];
        float rm22 = rotation[base + 8];

        rotate(target.pose(), source.pose(), m30, m31, m32, m33, properties, rm00, rm01, rm02, rm10, rm11, rm12, rm20, rm21, rm22);
        rotate(target.normal(), source.normal(), rm00, rm01, rm02, rm10, rm11, rm12, rm20, rm21, rm22);
    }

    private static int rotationSlot(float angleZ, float angleY, float angleX) {
        int bitsX = Float.floatToRawIntBits(angleX);
        int bitsY = Float.floatToRawIntBits(angleY);
        int bitsZ = Float.floatToRawIntBits(angleZ);
        int hash  = (bitsX * 31 + bitsY) * 31 + bitsZ;
        int slot  = (hash ^ hash >>> 16) & CACHE_SLOTS - 1;

        if (FILLED[slot] && CACHED_X[slot] == bitsX && CACHED_Y[slot] == bitsY && CACHED_Z[slot] == bitsZ)
            return slot;

        fillRotation(ROTATIONS, slot * ROTATION_VALUES, angleZ, angleY, angleX);
        CACHED_X[slot] = bitsX;
        CACHED_Y[slot] = bitsY;
        CACHED_Z[slot] = bitsZ;
        FILLED[slot]   = true;

        return slot;
    }

    private static void fillRotation(float[] rotation, int base, float angleZ, float angleY, float angleX) {
        float halfX = angleX * HALF;
        float halfY = angleY * HALF;
        float halfZ = angleZ * HALF;
        float sx    = sin(halfX);
        float cx    = cos(sx, halfX);
        float sy    = sin(halfY);
        float cy    = cos(sy, halfY);
        float sz    = sin(halfZ);
        float cz    = cos(sz, halfZ);
        float cycz  = cy * cz;
        float sysz  = sy * sz;
        float sycz  = sy * cz;
        float cysz  = cy * sz;
        float w     = cx * cycz + sx * sysz;
        float x     = sx * cycz - cx * sysz;
        float y     = cx * sycz + sx * cysz;
        float z     = cx * cysz - sx * sycz;
        float w2    = w * w;
        float x2    = x * x;
        float y2    = y * y;
        float z2    = z * z;
        float zw    = z * w;
        float dzw   = zw + zw;
        float xy    = x * y;
        float dxy   = xy + xy;
        float xz    = x * z;
        float dxz   = xz + xz;
        float yw    = y * w;
        float dyw   = yw + yw;
        float yz    = y * z;
        float dyz   = yz + yz;
        float xw    = x * w;
        float dxw   = xw + xw;

        rotation[base]     = w2 + x2 - z2 - y2;
        rotation[base + 1] = dxy + dzw;
        rotation[base + 2] = dxz - dyw;
        rotation[base + 3] = -dzw + dxy;
        rotation[base + 4] = y2 - z2 + w2 - x2;
        rotation[base + 5] = dyz + dxw;
        rotation[base + 6] = dyw + dxz;
        rotation[base + 7] = dyz - dxw;
        rotation[base + 8] = z2 - y2 - x2 + w2;
    }

    private static float sin(float angle) {
        if (angle == 0.0F && !Options.FASTMATH)
            return angle;

        return Math.sin(angle);
    }

    private static float cos(float sin, float angle) {
        if (angle == 0.0F && !Options.FASTMATH)
            return 1.0F;

        return Math.cosFromSin(sin, angle);
    }

    private static void rotate(Matrix4f target, Matrix4f source, float m30, float m31, float m32, float m33, int properties, float rm00, float rm01, float rm02, float rm10, float rm11, float rm12, float rm20, float rm21, float rm22) {
        if ((properties & Matrix4fc.PROPERTY_IDENTITY) != 0) {
            target.set(
                rm00, rm01, rm02, source.m03(),
                rm10, rm11, rm12, source.m13(),
                rm20, rm21, rm22, source.m23(),
                m30, m31, m32, m33
            );
            target.assume(ROTATION_PROPERTIES);

            return;
        }

        if ((properties & Matrix4fc.PROPERTY_TRANSLATION) != 0) {
            target.set(
                rm00, rm01, rm02, 0.0F,
                rm10, rm11, rm12, 0.0F,
                rm20, rm21, rm22, 0.0F,
                m30, m31, m32, m33
            );
            target.assume(properties & ROTATED_PROPERTIES);

            return;
        }

        if ((properties & Matrix4fc.PROPERTY_AFFINE) != 0) {
            target.set(
                source.m00() * rm00 + source.m10() * rm01 + source.m20() * rm02, source.m01() * rm00 + source.m11() * rm01 + source.m21() * rm02, source.m02() * rm00 + source.m12() * rm01 + source.m22() * rm02, 0.0F,
                source.m00() * rm10 + source.m10() * rm11 + source.m20() * rm12, source.m01() * rm10 + source.m11() * rm11 + source.m21() * rm12, source.m02() * rm10 + source.m12() * rm11 + source.m22() * rm12, 0.0F,
                source.m00() * rm20 + source.m10() * rm21 + source.m20() * rm22, source.m01() * rm20 + source.m11() * rm21 + source.m21() * rm22, source.m02() * rm20 + source.m12() * rm21 + source.m22() * rm22, 0.0F,
                m30, m31, m32, m33
            );
            target.assume(properties & ROTATED_PROPERTIES);

            return;
        }

        target.set(
            Math.fma(source.m00(), rm00, Math.fma(source.m10(), rm01, source.m20() * rm02)), Math.fma(source.m01(), rm00, Math.fma(source.m11(), rm01, source.m21() * rm02)), Math.fma(source.m02(), rm00, Math.fma(source.m12(), rm01, source.m22() * rm02)), Math.fma(source.m03(), rm00, Math.fma(source.m13(), rm01, source.m23() * rm02)),
            Math.fma(source.m00(), rm10, Math.fma(source.m10(), rm11, source.m20() * rm12)), Math.fma(source.m01(), rm10, Math.fma(source.m11(), rm11, source.m21() * rm12)), Math.fma(source.m02(), rm10, Math.fma(source.m12(), rm11, source.m22() * rm12)), Math.fma(source.m03(), rm10, Math.fma(source.m13(), rm11, source.m23() * rm12)),
            Math.fma(source.m00(), rm20, Math.fma(source.m10(), rm21, source.m20() * rm22)), Math.fma(source.m01(), rm20, Math.fma(source.m11(), rm21, source.m21() * rm22)), Math.fma(source.m02(), rm20, Math.fma(source.m12(), rm21, source.m22() * rm22)), Math.fma(source.m03(), rm20, Math.fma(source.m13(), rm21, source.m23() * rm22)),
            m30, m31, m32, m33
        );
        target.assume(properties & ROTATED_PROPERTIES);
    }

    private static void rotate(Matrix3f target, Matrix3f source, float rm00, float rm01, float rm02, float rm10, float rm11, float rm12, float rm20, float rm21, float rm22) {
        float nm00 = source.m00 * rm00 + source.m10 * rm01 + source.m20 * rm02;
        float nm01 = source.m01 * rm00 + source.m11 * rm01 + source.m21 * rm02;
        float nm02 = source.m02 * rm00 + source.m12 * rm01 + source.m22 * rm02;
        float nm10 = source.m00 * rm10 + source.m10 * rm11 + source.m20 * rm12;
        float nm11 = source.m01 * rm10 + source.m11 * rm11 + source.m21 * rm12;
        float nm12 = source.m02 * rm10 + source.m12 * rm11 + source.m22 * rm12;

        target.m20 = source.m00 * rm20 + source.m10 * rm21 + source.m20 * rm22;
        target.m21 = source.m01 * rm20 + source.m11 * rm21 + source.m21 * rm22;
        target.m22 = source.m02 * rm20 + source.m12 * rm21 + source.m22 * rm22;
        target.m00 = nm00;
        target.m01 = nm01;
        target.m02 = nm02;
        target.m10 = nm10;
        target.m11 = nm11;
        target.m12 = nm12;
    }
}
