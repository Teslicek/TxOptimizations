package com.teslicek.txoptimizations;

import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class PositionTransform {

    private PositionTransform() {
    }

    public static float x(Matrix4f matrix, int properties, float x, float y, float z) {
        if ((properties & Matrix4fc.PROPERTY_IDENTITY) != 0)
            return x;

        if ((properties & Matrix4fc.PROPERTY_TRANSLATION) != 0)
            return x + matrix.m30();

        return Math.fma(matrix.m00(), x, Math.fma(matrix.m10(), y, Math.fma(matrix.m20(), z, matrix.m30())));
    }

    public static float y(Matrix4f matrix, int properties, float x, float y, float z) {
        if ((properties & Matrix4fc.PROPERTY_IDENTITY) != 0)
            return y;

        if ((properties & Matrix4fc.PROPERTY_TRANSLATION) != 0)
            return y + matrix.m31();

        return Math.fma(matrix.m01(), x, Math.fma(matrix.m11(), y, Math.fma(matrix.m21(), z, matrix.m31())));
    }

    public static float z(Matrix4f matrix, int properties, float x, float y, float z) {
        if ((properties & Matrix4fc.PROPERTY_IDENTITY) != 0)
            return z;

        if ((properties & Matrix4fc.PROPERTY_TRANSLATION) != 0)
            return z + matrix.m32();

        return Math.fma(matrix.m02(), x, Math.fma(matrix.m12(), y, Math.fma(matrix.m22(), z, matrix.m32())));
    }
}
