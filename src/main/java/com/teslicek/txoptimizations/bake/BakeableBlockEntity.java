package com.teslicek.txoptimizations.bake;

public interface BakeableBlockEntity extends Bakeable {

    boolean txoptimizations$isRenderBoth();

    boolean txoptimizations$isHidden();

    boolean txoptimizations$isForcedEntity();

    void txoptimizations$setForcedEntity(boolean forcedEntity);

    long txoptimizations$getEmptyTick();

    boolean txoptimizations$isEmpty();

    void txoptimizations$setEmpty(long tick, boolean empty);
}
