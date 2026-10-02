package com.teslicek.txoptimizations.cull;

public interface Cullable {

    boolean txoptimizations$isCulled();

    void txoptimizations$setCulled(boolean culled);

    long txoptimizations$getVisibleUntil();

    void txoptimizations$setVisibleUntil(long visibleUntil);

    boolean txoptimizations$isOutOfCamera();

    void txoptimizations$setOutOfCamera(boolean outOfCamera);
}
