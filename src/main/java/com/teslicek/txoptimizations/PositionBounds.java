package com.teslicek.txoptimizations;

import org.lwjgl.system.MemoryUtil;

public final class PositionBounds {

    public static final int VALUES = 6;

    private PositionBounds() {
    }

    public static void measure(long address, int vertices, int stride, float[] boxes, int offset) {
        long  end   = address + (long) vertices * stride;
        float lowX  = Float.POSITIVE_INFINITY;
        float lowY  = Float.POSITIVE_INFINITY;
        float lowZ  = Float.POSITIVE_INFINITY;
        float highX = Float.NEGATIVE_INFINITY;
        float highY = Float.NEGATIVE_INFINITY;
        float highZ = Float.NEGATIVE_INFINITY;
        float sum   = 0.0F;

        for (long vertex = address; vertex < end; vertex += stride) {
            float x = MemoryUtil.memGetFloat(vertex);
            float y = MemoryUtil.memGetFloat(vertex + 4L);
            float z = MemoryUtil.memGetFloat(vertex + 8L);

            lowX  = x < lowX ? x : lowX;
            lowY  = y < lowY ? y : lowY;
            lowZ  = z < lowZ ? z : lowZ;
            highX = x > highX ? x : highX;
            highY = y > highY ? y : highY;
            highZ = z > highZ ? z : highZ;
            sum  += x + y + z;
        }

        if (!Float.isFinite(sum))
            lowX = Float.NaN;

        boxes[offset]     = lowX;
        boxes[offset + 1] = lowY;
        boxes[offset + 2] = lowZ;
        boxes[offset + 3] = highX;
        boxes[offset + 4] = highY;
        boxes[offset + 5] = highZ;
    }
}
