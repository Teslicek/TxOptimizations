package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.CompactVectorArray;
import com.mojang.blaze3d.vertex.VertexSorting;
import org.joml.Vector3f;

public final class DistanceSorting implements VertexSorting, SharedQuadSorter {

    private static final Vector3f SHARED_SCRATCH = new Vector3f();

    private final VertexSorting.DistanceFunction function;

    public DistanceSorting(VertexSorting.DistanceFunction function) {
        this.function = function;
    }

    @Override
    public int[] sort(CompactVectorArray points) {
        int   length = points.size();
        int[] keys   = new int[length];

        this.fillKeys(points, keys, length, new Vector3f());

        return QuadSort.sorted(keys, length);
    }

    @Override
    public int[] txoptimizations$sortShared(CompactVectorArray points) {
        int   length = points.size();
        int[] keys   = QuadSort.keys(length);

        this.fillKeys(points, keys, length, SHARED_SCRATCH);

        return QuadSort.sort(keys, length);
    }

    private void fillKeys(CompactVectorArray points, int[] keys, int length, Vector3f scratch) {
        for (int index = 0; index < length; index ++)
            keys[index] = QuadSort.key(this.function.apply(points.get(index, scratch)));
    }
}
