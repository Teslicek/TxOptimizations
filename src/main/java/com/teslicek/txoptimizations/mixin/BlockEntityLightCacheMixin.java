package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.BlockEntityLight;
import com.teslicek.txoptimizations.CachedBlockEntityLight;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockEntity.class)
public abstract class BlockEntityLightCacheMixin implements CachedBlockEntityLight {

    @Unique
    private @Nullable BlockState txoptimizations$lightState;

    @Unique
    private int txoptimizations$lightGeneration;

    @Unique
    private int txoptimizations$light;

    @Override
    public int txoptimizations$cachedLight(BlockState state, int generation) {
        if (state != this.txoptimizations$lightState || generation != this.txoptimizations$lightGeneration)
            return BlockEntityLight.NOT_CACHED;

        return this.txoptimizations$light;
    }

    @Override
    public void txoptimizations$cacheLight(BlockState state, int generation, int light) {
        this.txoptimizations$lightState      = state;
        this.txoptimizations$lightGeneration = generation;
        this.txoptimizations$light           = light;
    }
}
