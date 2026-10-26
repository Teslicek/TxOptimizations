package com.teslicek.txoptimizations;

import net.minecraft.world.level.block.state.BlockState;

public interface CachedBlockEntityLight {

    int txoptimizations$cachedLight(BlockState state, int generation);

    void txoptimizations$cacheLight(BlockState state, int generation, int light);
}
