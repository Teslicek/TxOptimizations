package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.BlockStateCacheBatch;
import net.minecraft.SharedConstants;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Blocks.class)
public abstract class BlocksParallelCacheMixin {

    @Redirect(method = "lambda$static$429", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;initCache()V"))
    private static void txoptimizations$deferCache(BlockState state) {
        if (SharedConstants.DEBUG_CALCULATE_SOLID) {
            state.initCache();
            return;
        }

        BlockStateCacheBatch.defer(state);
    }
}
