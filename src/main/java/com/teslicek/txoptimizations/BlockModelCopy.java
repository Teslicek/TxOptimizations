package com.teslicek.txoptimizations;

import net.minecraft.client.renderer.block.BlockModelRenderState;

public interface BlockModelCopy {

    boolean txoptimizations$copyable();

    boolean txoptimizations$hasAttachedData();

    void txoptimizations$copyFrom(BlockModelRenderState prepared);
}
