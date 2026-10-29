package com.teslicek.txoptimizations;

public interface BackgroundVertexCopy {

    void txoptimizations$copyVerticesInBackground();

    boolean txoptimizations$isCopyingInBackground();

    void txoptimizations$finishOutlineBounds();

    void txoptimizations$awaitVertexCopy();

    void txoptimizations$finishVertexCopy();
}
