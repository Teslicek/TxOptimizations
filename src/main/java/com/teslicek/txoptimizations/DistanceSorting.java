package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.CompactVectorArray;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

public final class DistanceSorting implements VertexSorting, SharedQuadSorter {

    private static final Vector3f SHARED_SCRATCH    = new Vector3f();
    private static final Vector3f ORIGIN            = new Vector3f();
    private static final int      VERTICES_PER_QUAD = PrimitiveTopology.QUADS.primitiveLength;

    private final VertexSorting.DistanceFunction function;

    public DistanceSorting(VertexSorting.DistanceFunction function) {
        this.function = function;
    }

    @Override
    public int[] sort(CompactVectorArray points) {
        int      length  = points.size();
        Vector3f scratch = new Vector3f();

        if (length < QuadSort.FEW_QUADS) {
            int[] keys = new int[length];

            this.fillKeys(points, keys, length, scratch);

            return QuadSort.stableOrder(keys);
        }

        long[] values = new long[length];

        for (int index = 0; index < length; index ++)
            values[index] = QuadSort.pack(this.key(points, index, scratch), index);

        return QuadSort.order(values);
    }

    @Override
    public int[] txoptimizations$sortShared(CompactVectorArray points) {
        int   length = points.size();
        int[] keys   = QuadSort.keys(length);

        this.fillKeys(points, keys, length, SHARED_SCRATCH);

        return QuadSort.sort(keys, length);
    }

    public static int fillOriginKeys(long positions, int vertexSize, int quads, int[] keys, int firstQuad) {
        long quadSize = (long) vertexSize * VERTICES_PER_QUAD;
        long third    = 2L * vertexSize;

        for (int quad = 0; quad < quads; quad ++) {
            long first = positions + quad * quadSize;

            keys[firstQuad + quad] = originKey(centroid(first, first + third), centroid(first + Float.BYTES, first + third + Float.BYTES), centroid(first + 2L * Float.BYTES, first + third + 2L * Float.BYTES));
        }

        return firstQuad + quads;
    }

    public static int originKey(float x, float y, float z) {
        return QuadSort.key(ORIGIN.distanceSquared(x, y, z));
    }

    public static float centroid(long first, long third) {
        return (MemoryUtil.memGetFloat(first) + MemoryUtil.memGetFloat(third)) / 2.0F;
    }

    private void fillKeys(CompactVectorArray points, int[] keys, int length, Vector3f scratch) {
        for (int index = 0; index < length; index ++)
            keys[index] = this.key(points, index, scratch);
    }

    private int key(CompactVectorArray points, int index, Vector3f scratch) {
        return QuadSort.key(this.function.apply(points.get(index, scratch)));
    }
}
