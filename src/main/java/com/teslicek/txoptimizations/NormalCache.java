package com.teslicek.txoptimizations;

import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import org.joml.Matrix3f;

public final class NormalCache {

    private static final int   ENTRIES = 8;
    private static final int[] MATRIX  = new int[9];
    private static final int[] NORMALS = new int[ENTRIES];
    private static final int[] RESULTS = new int[ENTRIES];

    private static boolean valid;
    private static boolean trusted;
    private static int     count;
    private static int     next;

    private NormalCache() {
    }

    public static boolean usable() {
        RenderThreadStack.Owner owner = RenderThreadStack.owner();

        return owner != null && owner.thread() == Thread.currentThread();
    }

    public static void select(Matrix3f matrix, boolean trustedNormals) {
        if (valid && trusted == trustedNormals && matches(matrix))
            return;

        MATRIX[0] = Float.floatToRawIntBits(matrix.m00);
        MATRIX[1] = Float.floatToRawIntBits(matrix.m01);
        MATRIX[2] = Float.floatToRawIntBits(matrix.m02);
        MATRIX[3] = Float.floatToRawIntBits(matrix.m10);
        MATRIX[4] = Float.floatToRawIntBits(matrix.m11);
        MATRIX[5] = Float.floatToRawIntBits(matrix.m12);
        MATRIX[6] = Float.floatToRawIntBits(matrix.m20);
        MATRIX[7] = Float.floatToRawIntBits(matrix.m21);
        MATRIX[8] = Float.floatToRawIntBits(matrix.m22);
        valid     = true;
        trusted   = trustedNormals;
        count     = 0;
        next      = 0;
    }

    public static int transform(Matrix3f matrix, int normal) {
        for (int index = 0; index < count; index ++) {
            if (NORMALS[index] == normal)
                return RESULTS[index];
        }

        int result = MatrixHelper.transformNormal(matrix, trusted, normal);

        NORMALS[next] = normal;
        RESULTS[next] = result;
        next          = (next + 1) % ENTRIES;

        if (count < ENTRIES)
            count ++;

        return result;
    }

    private static boolean matches(Matrix3f matrix) {
        return MATRIX[0] == Float.floatToRawIntBits(matrix.m00)
            && MATRIX[1] == Float.floatToRawIntBits(matrix.m01)
            && MATRIX[2] == Float.floatToRawIntBits(matrix.m02)
            && MATRIX[3] == Float.floatToRawIntBits(matrix.m10)
            && MATRIX[4] == Float.floatToRawIntBits(matrix.m11)
            && MATRIX[5] == Float.floatToRawIntBits(matrix.m12)
            && MATRIX[6] == Float.floatToRawIntBits(matrix.m20)
            && MATRIX[7] == Float.floatToRawIntBits(matrix.m21)
            && MATRIX[8] == Float.floatToRawIntBits(matrix.m22);
    }
}
