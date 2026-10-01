package com.teslicek.txoptimizations.bake;

public interface Bakeable {

    boolean txoptimizations$isBakeSupported();

    void txoptimizations$setBakeSupported(boolean supported);

    RenderMode txoptimizations$getRenderMode();

    void txoptimizations$setRenderMode(RenderMode mode);

    RenderMode txoptimizations$getPendingMode();

    void txoptimizations$setPendingMode(RenderMode mode);
}
