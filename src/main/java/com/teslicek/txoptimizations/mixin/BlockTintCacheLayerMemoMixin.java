package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.TintLayerMemo;
import net.minecraft.client.color.block.BlockTintCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockTintCache.class)
public abstract class BlockTintCacheLayerMemoMixin {

    @Redirect(method = "getColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/color/block/BlockTintCache$CacheData;getLayer(I)[I"))
    private int[] txoptimizations$memoizedLayer(BlockTintCache.CacheData cache, int y, @Local BlockTintCache.LatestCacheInfo chunkInfo) {
        return ((TintLayerMemo) chunkInfo).txoptimizations$layer(cache, y);
    }
}
