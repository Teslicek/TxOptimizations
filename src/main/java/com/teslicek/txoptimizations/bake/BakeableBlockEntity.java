package com.teslicek.txoptimizations.bake;

public interface BakeableBlockEntity extends Bakeable {

    boolean txoptimizations$isRenderBoth();

    void txoptimizations$setRenderBoth(boolean renderBoth);

    boolean txoptimizations$isHidden();

    void txoptimizations$setHidden(boolean hidden);

    boolean txoptimizations$isForcedEntity();

    void txoptimizations$setForcedEntity(boolean forcedEntity);

    boolean txoptimizations$isTimerFinished();

    void txoptimizations$setTimer(long start, int duration);
}
