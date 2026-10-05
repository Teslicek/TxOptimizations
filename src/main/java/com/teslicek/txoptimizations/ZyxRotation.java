package com.teslicek.txoptimizations;

import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Options;

public final class ZyxRotation {

    private static final int ROTATION_PROPERTIES = Matrix4fc.PROPERTY_AFFINE | Matrix4fc.PROPERTY_ORTHONORMAL;
    private static final int ROTATED_PROPERTIES  = ~(Matrix4fc.PROPERTY_PERSPECTIVE | Matrix4fc.PROPERTY_IDENTITY | Matrix4fc.PROPERTY_TRANSLATION);

    private ZyxRotation() {
    }

    public static void rotate(Matrix4f pose, Matrix3f normal, float angleZ, float angleY, float angleX) {
        float sinX = sin(angleX);
        float cosX = cos(sinX, angleX);
        float sinY = sin(angleY);
        float cosY = cos(sinY, angleY);
        float sinZ = sin(angleZ);
        float cosZ = cos(sinZ, angleZ);

        rotate(pose, sinX, cosX, sinY, cosY, sinZ, cosZ);
        rotate(normal, sinX, cosX, sinY, cosY, sinZ, cosZ);
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

    private static void rotate(Matrix4f matrix, float sinX, float cosX, float sinY, float cosY, float sinZ, float cosZ) {
        int properties = matrix.properties();

        if ((properties & Matrix4fc.PROPERTY_IDENTITY) != 0) {
            rotation(matrix, sinX, cosX, sinY, cosY, sinZ, cosZ);

            return;
        }

        if ((properties & Matrix4fc.PROPERTY_TRANSLATION) != 0) {
            float translationX = matrix.m30();
            float translationY = matrix.m31();
            float translationZ = matrix.m32();

            rotation(matrix, sinX, cosX, sinY, cosY, sinZ, cosZ);
            matrix.setTranslation(translationX, translationY, translationZ);

            return;
        }

        float mSinZ = -sinZ;
        float mSinY = -sinY;
        float mSinX = -sinX;
        float nm00  = matrix.m00() * cosZ + matrix.m10() * sinZ;
        float nm01  = matrix.m01() * cosZ + matrix.m11() * sinZ;
        float nm02  = matrix.m02() * cosZ + matrix.m12() * sinZ;
        float nm10  = matrix.m00() * mSinZ + matrix.m10() * cosZ;
        float nm11  = matrix.m01() * mSinZ + matrix.m11() * cosZ;
        float nm12  = matrix.m02() * mSinZ + matrix.m12() * cosZ;
        float nm20  = nm00 * sinY + matrix.m20() * cosY;
        float nm21  = nm01 * sinY + matrix.m21() * cosY;
        float nm22  = nm02 * sinY + matrix.m22() * cosY;

        if ((properties & Matrix4fc.PROPERTY_AFFINE) != 0) {
            matrix.set(
                nm00 * cosY + matrix.m20() * mSinY, nm01 * cosY + matrix.m21() * mSinY, nm02 * cosY + matrix.m22() * mSinY, 0.0F,
                nm10 * cosX + nm20 * sinX, nm11 * cosX + nm21 * sinX, nm12 * cosX + nm22 * sinX, 0.0F,
                nm10 * mSinX + nm20 * cosX, nm11 * mSinX + nm21 * cosX, nm12 * mSinX + nm22 * cosX, 0.0F,
                matrix.m30(), matrix.m31(), matrix.m32(), matrix.m33()
            );
            matrix.assume(properties & ROTATED_PROPERTIES);

            return;
        }

        float nm03 = matrix.m03() * cosZ + matrix.m13() * sinZ;
        float nm13 = matrix.m03() * mSinZ + matrix.m13() * cosZ;
        float nm23 = nm03 * sinY + matrix.m23() * cosY;

        matrix.set(
            nm00 * cosY + matrix.m20() * mSinY, nm01 * cosY + matrix.m21() * mSinY, nm02 * cosY + matrix.m22() * mSinY, nm03 * cosY + matrix.m23() * mSinY,
            nm10 * cosX + nm20 * sinX, nm11 * cosX + nm21 * sinX, nm12 * cosX + nm22 * sinX, nm13 * cosX + nm23 * sinX,
            nm10 * mSinX + nm20 * cosX, nm11 * mSinX + nm21 * cosX, nm12 * mSinX + nm22 * cosX, nm13 * mSinX + nm23 * cosX,
            matrix.m30(), matrix.m31(), matrix.m32(), matrix.m33()
        );
        matrix.assume(properties & ROTATED_PROPERTIES);
    }

    private static void rotation(Matrix4f matrix, float sinX, float cosX, float sinY, float cosY, float sinZ, float cosZ) {
        float nm20 = cosZ * sinY;
        float nm21 = sinZ * sinY;

        matrix.set(
            cosZ * cosY, sinZ * cosY, -sinY, 0.0F,
            -sinZ * cosX + nm20 * sinX, cosZ * cosX + nm21 * sinX, cosY * sinX, 0.0F,
            -sinZ * -sinX + nm20 * cosX, cosZ * -sinX + nm21 * cosX, cosY * cosX, 0.0F,
            0.0F, 0.0F, 0.0F, 1.0F
        );
        matrix.assume(ROTATION_PROPERTIES);
    }

    private static void rotate(Matrix3f matrix, float sinX, float cosX, float sinY, float cosY, float sinZ, float cosZ) {
        float mSinZ = -sinZ;
        float mSinY = -sinY;
        float mSinX = -sinX;
        float nm00  = matrix.m00 * cosZ + matrix.m10 * sinZ;
        float nm01  = matrix.m01 * cosZ + matrix.m11 * sinZ;
        float nm02  = matrix.m02 * cosZ + matrix.m12 * sinZ;
        float nm10  = matrix.m00 * mSinZ + matrix.m10 * cosZ;
        float nm11  = matrix.m01 * mSinZ + matrix.m11 * cosZ;
        float nm12  = matrix.m02 * mSinZ + matrix.m12 * cosZ;
        float nm20  = nm00 * sinY + matrix.m20 * cosY;
        float nm21  = nm01 * sinY + matrix.m21 * cosY;
        float nm22  = nm02 * sinY + matrix.m22 * cosY;

        matrix.m00 = nm00 * cosY + matrix.m20 * mSinY;
        matrix.m01 = nm01 * cosY + matrix.m21 * mSinY;
        matrix.m02 = nm02 * cosY + matrix.m22 * mSinY;
        matrix.m10 = nm10 * cosX + nm20 * sinX;
        matrix.m11 = nm11 * cosX + nm21 * sinX;
        matrix.m12 = nm12 * cosX + nm22 * sinX;
        matrix.m20 = nm10 * mSinX + nm20 * cosX;
        matrix.m21 = nm11 * mSinX + nm21 * cosX;
        matrix.m22 = nm12 * mSinX + nm22 * cosX;
    }
}
