package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.TintLayerMemo;
import java.util.Arrays;
import net.minecraft.client.color.block.BlockTintCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockTintCache.LatestCacheInfo.class)
public abstract class BlockTintCacheLatestCacheInfoMixin implements TintLayerMemo {

    @Unique
    private static final int SLOTS = 16;

    @Unique
    private final int[] txoptimizations$layerYs = new int[SLOTS];

    @Unique
    private final int[][] txoptimizations$layers = new int[SLOTS][];

    @Unique
    private BlockTintCache.CacheData txoptimizations$layersOf;

    @Override
    public int[] txoptimizations$layer(BlockTintCache.CacheData cache, int y) {
        if (cache != this.txoptimizations$layersOf) {
            Arrays.fill(this.txoptimizations$layers, null);
            this.txoptimizations$layersOf = cache;
        }

        int   slot  = y & (SLOTS - 1);
        int[] layer = this.txoptimizations$layers[slot];

        if (layer != null && this.txoptimizations$layerYs[slot] == y)
            return layer;

        layer = cache.getLayer(y);

        this.txoptimizations$layers[slot]  = layer;
        this.txoptimizations$layerYs[slot] = y;

        return layer;
    }
}
