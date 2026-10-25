package com.teslicek.txoptimizations;

public interface BackgroundVertexCopy {

    void txoptimizations$copyVerticesInBackground();

    void txoptimizations$finishOutlineBounds();

    void txoptimizations$awaitVertexCopy();

    void txoptimizations$finishVertexCopy();
}
