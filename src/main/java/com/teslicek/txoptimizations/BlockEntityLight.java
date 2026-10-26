package com.teslicek.txoptimizations;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;

public final class BlockEntityLight {

    public static final int NOT_CACHED = -1;

    private static volatile int generation = 1;

    private BlockEntityLight() {
    }

    public static void changed() {
        generation ++;
    }

    public static int lightCoords(BlockAndLightGetter level, BlockPos pos, BlockEntity blockEntity) {
        if (!(level instanceof ClientLevel client) || client.getLightEngine().getClass() != LevelLightEngine.class)
            return LightCoordsUtil.getLightCoords(level, pos);

        CachedBlockEntityLight cache   = (CachedBlockEntityLight) blockEntity;
        BlockState             state   = blockEntity.getBlockState();
        int                    current = generation;
        int                    light   = cache.txoptimizations$cachedLight(state, current);

        if (light != NOT_CACHED)
            return light;

        light = LightCoordsUtil.getLightCoords(level, pos);
        cache.txoptimizations$cacheLight(state, current, light);

        return light;
    }
}
