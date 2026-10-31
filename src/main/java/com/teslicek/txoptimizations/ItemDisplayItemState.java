package com.teslicek.txoptimizations;

import net.minecraft.client.renderer.item.ItemStackRenderState;

public interface ItemDisplayItemState {

    ItemStackRenderState txoptimizations$reusedItemState();

    boolean txoptimizations$resolvedFor(Object item, Object level, Object models);

    void txoptimizations$setResolved(Object item, Object level, Object models, boolean pure);
}
