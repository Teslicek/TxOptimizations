package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Options;

public final class ModelRotation {

    private static final float HALF                = 0.5F;
    private static final int   ROTATION_PROPERTIES = Matrix4fc.PROPERTY_AFFINE | Matrix4fc.PROPERTY_ORTHONORMAL;
    private static final int   ROTATED_PROPERTIES  = ~(Matrix4fc.PROPERTY_PERSPECTIVE | Matrix4fc.PROPERTY_IDENTITY | Matrix4fc.PROPERTY_TRANSLATION);

    private ModelRotation() {
    }

    public static void rotateZYX(PoseStack.Pose pose, float angleZ, float angleY, float angleX) {
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
        float rm00  = w2 + x2 - z2 - y2;
        float rm01  = dxy + dzw;
        float rm02  = dxz - dyw;
        float rm10  = -dzw + dxy;
        float rm11  = y2 - z2 + w2 - x2;
        float rm12  = dyz + dxw;
        float rm20  = dyw + dxz;
        float rm21  = dyz - dxw;
        float rm22  = z2 - y2 - x2 + w2;

        rotate(pose.pose(), rm00, rm01, rm02, rm10, rm11, rm12, rm20, rm21, rm22);
        rotate(pose.normal(), rm00, rm01, rm02, rm10, rm11, rm12, rm20, rm21, rm22);
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

    private static void rotate(Matrix4f matrix, float rm00, float rm01, float rm02, float rm10, float rm11, float rm12, float rm20, float rm21, float rm22) {
        int properties = matrix.properties();

        if ((properties & Matrix4fc.PROPERTY_IDENTITY) != 0) {
            matrix.set(
                rm00, rm01, rm02, matrix.m03(),
                rm10, rm11, rm12, matrix.m13(),
                rm20, rm21, rm22, matrix.m23(),
                matrix.m30(), matrix.m31(), matrix.m32(), matrix.m33()
            );
            matrix.assume(ROTATION_PROPERTIES);

            return;
        }

        if ((properties & Matrix4fc.PROPERTY_TRANSLATION) != 0) {
            matrix.set(
                rm00, rm01, rm02, 0.0F,
                rm10, rm11, rm12, 0.0F,
                rm20, rm21, rm22, 0.0F,
                matrix.m30(), matrix.m31(), matrix.m32(), matrix.m33()
            );
            matrix.assume(properties & ROTATED_PROPERTIES);

            return;
        }

        if ((properties & Matrix4fc.PROPERTY_AFFINE) != 0) {
            matrix.set(
                matrix.m00() * rm00 + matrix.m10() * rm01 + matrix.m20() * rm02, matrix.m01() * rm00 + matrix.m11() * rm01 + matrix.m21() * rm02, matrix.m02() * rm00 + matrix.m12() * rm01 + matrix.m22() * rm02, 0.0F,
                matrix.m00() * rm10 + matrix.m10() * rm11 + matrix.m20() * rm12, matrix.m01() * rm10 + matrix.m11() * rm11 + matrix.m21() * rm12, matrix.m02() * rm10 + matrix.m12() * rm11 + matrix.m22() * rm12, 0.0F,
                matrix.m00() * rm20 + matrix.m10() * rm21 + matrix.m20() * rm22, matrix.m01() * rm20 + matrix.m11() * rm21 + matrix.m21() * rm22, matrix.m02() * rm20 + matrix.m12() * rm21 + matrix.m22() * rm22, 0.0F,
                matrix.m30(), matrix.m31(), matrix.m32(), matrix.m33()
            );
            matrix.assume(properties & ROTATED_PROPERTIES);

            return;
        }

        matrix.set(
            Math.fma(matrix.m00(), rm00, Math.fma(matrix.m10(), rm01, matrix.m20() * rm02)), Math.fma(matrix.m01(), rm00, Math.fma(matrix.m11(), rm01, matrix.m21() * rm02)), Math.fma(matrix.m02(), rm00, Math.fma(matrix.m12(), rm01, matrix.m22() * rm02)), Math.fma(matrix.m03(), rm00, Math.fma(matrix.m13(), rm01, matrix.m23() * rm02)),
            Math.fma(matrix.m00(), rm10, Math.fma(matrix.m10(), rm11, matrix.m20() * rm12)), Math.fma(matrix.m01(), rm10, Math.fma(matrix.m11(), rm11, matrix.m21() * rm12)), Math.fma(matrix.m02(), rm10, Math.fma(matrix.m12(), rm11, matrix.m22() * rm12)), Math.fma(matrix.m03(), rm10, Math.fma(matrix.m13(), rm11, matrix.m23() * rm12)),
            Math.fma(matrix.m00(), rm20, Math.fma(matrix.m10(), rm21, matrix.m20() * rm22)), Math.fma(matrix.m01(), rm20, Math.fma(matrix.m11(), rm21, matrix.m21() * rm22)), Math.fma(matrix.m02(), rm20, Math.fma(matrix.m12(), rm21, matrix.m22() * rm22)), Math.fma(matrix.m03(), rm20, Math.fma(matrix.m13(), rm21, matrix.m23() * rm22)),
            matrix.m30(), matrix.m31(), matrix.m32(), matrix.m33()
        );
        matrix.assume(properties & ROTATED_PROPERTIES);
    }

    private static void rotate(Matrix3f matrix, float rm00, float rm01, float rm02, float rm10, float rm11, float rm12, float rm20, float rm21, float rm22) {
        float nm00 = matrix.m00 * rm00 + matrix.m10 * rm01 + matrix.m20 * rm02;
        float nm01 = matrix.m01 * rm00 + matrix.m11 * rm01 + matrix.m21 * rm02;
        float nm02 = matrix.m02 * rm00 + matrix.m12 * rm01 + matrix.m22 * rm02;
        float nm10 = matrix.m00 * rm10 + matrix.m10 * rm11 + matrix.m20 * rm12;
        float nm11 = matrix.m01 * rm10 + matrix.m11 * rm11 + matrix.m21 * rm12;
        float nm12 = matrix.m02 * rm10 + matrix.m12 * rm11 + matrix.m22 * rm12;

        matrix.m20 = matrix.m00 * rm20 + matrix.m10 * rm21 + matrix.m20 * rm22;
        matrix.m21 = matrix.m01 * rm20 + matrix.m11 * rm21 + matrix.m21 * rm22;
        matrix.m22 = matrix.m02 * rm20 + matrix.m12 * rm21 + matrix.m22 * rm22;
        matrix.m00 = nm00;
        matrix.m01 = nm01;
        matrix.m02 = nm02;
        matrix.m10 = nm10;
        matrix.m11 = nm11;
        matrix.m12 = nm12;
    }
}
