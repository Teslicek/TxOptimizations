package com.teslicek.txoptimizations;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ShadowBlockCache {

    private static final int   MIN_BRIGHTNESS = 3;
    private static final Block NONE           = new Block(null, 0);

    private static final Long2ObjectOpenHashMap<Block> BLOCKS = new Long2ObjectOpenHashMap<>();

    private static long  tick = -1L;
    private static Level level;

    private ShadowBlockCache() {
    }

    public static Block get(Level currentLevel, BlockPos pos, ChunkAccess chunk) {
        long currentTick = ClientClock.tick();

        if (currentTick != tick || currentLevel != level) {
            BLOCKS.clear();
            tick  = currentTick;
            level = currentLevel;
        }

        long  key   = pos.asLong();
        Block block = BLOCKS.get(key);

        if (block == null) {
            block = compute(currentLevel, pos, chunk);
            BLOCKS.put(key, block);
        }

        return block == NONE ? null : block;
    }

    private static Block compute(Level currentLevel, BlockPos pos, ChunkAccess chunk) {
        BlockPos   belowPos   = pos.below();
        BlockState belowState = chunk.getBlockState(belowPos);

        if (belowState.getRenderShape() == RenderShape.INVISIBLE)
            return NONE;

        int brightness = currentLevel.getMaxLocalRawBrightness(pos);

        if (brightness <= MIN_BRIGHTNESS || !belowState.isCollisionShapeFullBlock(chunk, belowPos))
            return NONE;

        VoxelShape shape = belowState.getShape(chunk, belowPos);

        return shape.isEmpty() ? NONE : new Block(shape, brightness);
    }

    public record Block(VoxelShape shape, int brightness) {
    }
}
