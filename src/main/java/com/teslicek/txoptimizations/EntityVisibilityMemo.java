package com.teslicek.txoptimizations;

public interface EntityVisibilityMemo {

    boolean txoptimizations$hasVisibility(long frame, float partialTick);

    boolean txoptimizations$isVisible();

    void txoptimizations$setVisibility(long frame, float partialTick, boolean visible);
}
