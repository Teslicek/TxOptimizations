package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import com.teslicek.txoptimizations.mixin.BufferBuilderAccessor;
import org.joml.Math;
import org.joml.Matrix4fc;

public final class QuadVertices {

    public static final int COUNT = 4;

    private QuadVertices() {
    }

    public static void putPosition(long pointer, Matrix4fc matrix, float x, float y, float z) {
        int properties = matrix.properties();

        if ((properties & Matrix4fc.PROPERTY_IDENTITY) != 0) {
            BufferBuilderAccessor.txoptimizations$putVec3f(pointer, x, y, z);
            return;
        }

        if ((properties & Matrix4fc.PROPERTY_TRANSLATION) != 0) {
            BufferBuilderAccessor.txoptimizations$putVec3f(pointer, x + matrix.m30(), y + matrix.m31(), z + matrix.m32());
            return;
        }

        float transformedX = Math.fma(matrix.m00(), x, Math.fma(matrix.m10(), y, Math.fma(matrix.m20(), z, matrix.m30())));
        float transformedY = Math.fma(matrix.m01(), x, Math.fma(matrix.m11(), y, Math.fma(matrix.m21(), z, matrix.m31())));
        float transformedZ = Math.fma(matrix.m02(), x, Math.fma(matrix.m12(), y, Math.fma(matrix.m22(), z, matrix.m32())));

        BufferBuilderAccessor.txoptimizations$putVec3f(pointer, transformedX, transformedY, transformedZ);
    }

    public static int offset(VertexFormat format, String name) {
        VertexFormatElement element = format.getElement(name);

        if (element == null)
            throw new IllegalStateException(format + " has no " + name + " element");

        return element.offset();
    }
}
