package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.BlockStateCacheDedup;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateCacheDedupMixin {

    @Shadow
    private BlockBehaviour.BlockStateBase.Cache cache;

    @Inject(method = "initCache", at = @At("TAIL"))
    private void txoptimizations$deduplicateCache(CallbackInfo ci) {
        if (this.cache == null)
            return;

        BlockStateCacheDedup.deduplicate(this.cache);
    }
}
