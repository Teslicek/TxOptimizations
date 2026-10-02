package com.teslicek.txoptimizations.bake;

public interface Bakeable {

    boolean txoptimizations$isBakeSupported();

    RenderMode txoptimizations$getRenderMode();

    void txoptimizations$setRenderMode(RenderMode mode);

    RenderMode txoptimizations$getPendingMode();

    void txoptimizations$setPendingMode(RenderMode mode);
}
