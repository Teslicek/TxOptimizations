package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.Direction;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Vector3fc;

public final class NormalCache {

    private static final Direction[] DIRECTIONS = Direction.values();
    private static final int[]       OPPOSITES  = opposites();
    private static final Direction[] AXES       = {Direction.DOWN, Direction.NORTH, Direction.WEST};
    private static final int         ALL        = (1 << DIRECTIONS.length) - 1;
    private static final int[]       MATRIX     = new int[9];
    private static final int[]       PACKED     = new int[DIRECTIONS.length];

    private static boolean valid;
    private static boolean trusted;
    private static int     known;

    private NormalCache() {
    }

    public static boolean usable() {
        return RenderThreadStack.isOwner();
    }

    public static void select(PoseStack.Pose pose) {
        Matrix3f matrix = pose.normal();

        if (valid && trusted == pose.trustedNormals && matches(matrix))
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
        trusted   = pose.trustedNormals;
        known     = 0;
    }

    public static void selectAll(PoseStack.Pose pose) {
        select(pose);

        if (known == ALL)
            return;

        for (Direction direction : AXES) {
            int index  = direction.ordinal();
            int normal = pack(pose, direction.getUnitVec3f());

            PACKED[index]            = normal;
            PACKED[OPPOSITES[index]] = VertexBytes.flipNormal(normal);
        }

        known = ALL;
    }

    public static int selected(int direction) {
        if ((known & 1 << direction) == 0)
            throw new IllegalStateException("No packed normal is selected for " + DIRECTIONS[direction]);

        return PACKED[direction];
    }

    public static int packed(PoseStack.Pose pose, Vector3fc normal) {
        for (Direction direction : DIRECTIONS) {
            if (direction.getUnitVec3f() == normal)
                return packed(pose, direction);
        }

        return pack(pose, normal);
    }

    public static int packed(PoseStack.Pose pose, Direction direction) {
        int index = direction.ordinal();
        int bit   = 1 << index;

        if ((known & bit) != 0)
            return PACKED[index];

        int opposite = OPPOSITES[index];
        int normal   = (known & 1 << opposite) != 0 ? VertexBytes.flipNormal(PACKED[opposite]) : pack(pose, direction.getUnitVec3f());

        PACKED[index] = normal;
        known        |= bit;

        return normal;
    }

    public static int pack(PoseStack.Pose pose, Vector3fc normal) {
        Matrix3f matrix = pose.normal();
        float    x      = normal.x();
        float    y      = normal.y();
        float    z      = normal.z();
        float    nx     = Math.fma(matrix.m00, x, Math.fma(matrix.m10, y, matrix.m20 * z));
        float    ny     = Math.fma(matrix.m01, x, Math.fma(matrix.m11, y, matrix.m21 * z));
        float    nz     = Math.fma(matrix.m02, x, Math.fma(matrix.m12, y, matrix.m22 * z));

        if (pose.trustedNormals)
            return VertexBytes.packNormal(nx, ny, nz);

        float scalar = Math.invsqrt(Math.fma(nx, nx, Math.fma(ny, ny, nz * nz)));

        return VertexBytes.packNormal(nx * scalar, ny * scalar, nz * scalar);
    }

    private static boolean matches(Matrix3f matrix) {
        int difference = MATRIX[0] ^ Float.floatToRawIntBits(matrix.m00)
                       | MATRIX[1] ^ Float.floatToRawIntBits(matrix.m01)
                       | MATRIX[2] ^ Float.floatToRawIntBits(matrix.m02)
                       | MATRIX[3] ^ Float.floatToRawIntBits(matrix.m10)
                       | MATRIX[4] ^ Float.floatToRawIntBits(matrix.m11)
                       | MATRIX[5] ^ Float.floatToRawIntBits(matrix.m12)
                       | MATRIX[6] ^ Float.floatToRawIntBits(matrix.m20)
                       | MATRIX[7] ^ Float.floatToRawIntBits(matrix.m21)
                       | MATRIX[8] ^ Float.floatToRawIntBits(matrix.m22);

        return difference == 0;
    }

    private static int[] opposites() {
        int[] opposites = new int[DIRECTIONS.length];

        for (Direction direction : DIRECTIONS)
            opposites[direction.ordinal()] = direction.getOpposite().ordinal();

        return opposites;
    }
}
