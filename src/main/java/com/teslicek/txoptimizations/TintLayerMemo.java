package com.teslicek.txoptimizations;

import net.minecraft.client.color.block.BlockTintCache;

public interface TintLayerMemo {

    int[] txoptimizations$layer(BlockTintCache.CacheData cache, int y);
}
