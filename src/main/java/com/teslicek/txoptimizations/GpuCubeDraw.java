package com.teslicek.txoptimizations;

import net.minecraft.client.renderer.rendertype.PreparedRenderType;

public interface GpuCubeDraw {

    void txoptimizations$setBatch(GpuCubeBatch batch);

    void txoptimizations$setPrepared(PreparedRenderType prepared);

    boolean txoptimizations$acceptsCubes();

    GpuCubeBatch txoptimizations$batch();

    int txoptimizations$rawVertices();

    void txoptimizations$addCube(int ref, int rawVertices);

    boolean txoptimizations$finishSegments();

    int txoptimizations$segments();

    boolean txoptimizations$isCubeSegment(int segment);

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
