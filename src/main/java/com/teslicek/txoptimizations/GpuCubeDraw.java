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
}
