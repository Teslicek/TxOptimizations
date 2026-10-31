package com.teslicek.txoptimizations;

import net.minecraft.client.renderer.rendertype.PreparedRenderType;

public interface GpuCubeDraw {

    int RAW   = 0;
    int CUBES = 1;
    int QUADS = 2;

    void txoptimizations$setBatch(GpuCubeBatch batch);

    void txoptimizations$setPrepared(PreparedRenderType prepared);

    boolean txoptimizations$acceptsCubes();

    GpuCubeBatch txoptimizations$batch();

    int txoptimizations$rawVertices();

    void txoptimizations$addRefs(int kind, int firstRef, int count, int rawVertices);

    boolean txoptimizations$finishSegments();

    int txoptimizations$segments();

    int txoptimizations$segmentKind(int segment);

    int txoptimizations$segmentStart(int segment);

    int txoptimizations$segmentLength(int segment);

    boolean txoptimizations$isSorted();

    void txoptimizations$addSortedKeys(int ref, int[] keys);

    int txoptimizations$sortedCubes();

    int[] txoptimizations$sortedRefs();

    int[] txoptimizations$sortedKeys();

    void txoptimizations$setSortedDraw(boolean expanded, int firstIndex, int indexCount, long vertexOffset, long vertexBytes);

    int txoptimizations$sortedMode();

    int txoptimizations$firstIndex();

    int txoptimizations$indexCount();

    long txoptimizations$vertexOffset();

    long txoptimizations$vertexBytes();
}
