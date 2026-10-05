package com.teslicek.txoptimizations;

import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;

public final class AirCollision {

    private static final double EPSILON = 1.0E-7;

    private AirCollision() {
    }

    public static boolean hasNoBlockColliders(Level level, AABB box) {
        if (level.isDebug())
            return false;

        int minX = Mth.floor(box.minX - EPSILON) - 1;
        int maxX = Mth.floor(box.maxX + EPSILON) + 1;
        int minY = Mth.floor(box.minY - EPSILON) - 1;
        int maxY = Mth.floor(box.maxY + EPSILON) + 1;
        int minZ = Mth.floor(box.minZ - EPSILON) - 1;
        int maxZ = Mth.floor(box.maxZ + EPSILON) + 1;

        return hasNoColliders(level, minX, maxX, minY, maxY, minZ, maxZ, false) && hasNoColliders(level, minX, maxX, minY, maxY, minZ, maxZ, true);
    }

    private static boolean hasNoColliders(Level level, int minX, int maxX, int minY, int maxY, int minZ, int maxZ, boolean ring) {
        int fromX = ring ? minX : minX + 1;
        int toX   = ring ? maxX : maxX - 1;
        int fromZ = ring ? minZ : minZ + 1;
        int toZ   = ring ? maxZ : maxZ - 1;

        for (int chunkX = SectionPos.blockToSectionCoord(fromX); chunkX <= SectionPos.blockToSectionCoord(toX); chunkX ++) {
            for (int chunkZ = SectionPos.blockToSectionCoord(fromZ); chunkZ <= SectionPos.blockToSectionCoord(toZ); chunkZ ++) {
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);

                if (chunk != null && !hasNoChunkColliders(chunk, minX, maxX, minY, maxY, minZ, maxZ, Math.max(fromX, chunkX << 4), Math.min(toX, (chunkX << 4) + 15), Math.max(fromZ, chunkZ << 4), Math.min(toZ, (chunkZ << 4) + 15), ring))
                    return false;
            }
        }

        return true;
    }

    private static boolean hasNoChunkColliders(ChunkAccess chunk, int minX, int maxX, int minY, int maxY, int minZ, int maxZ, int fromX, int toX, int fromZ, int toZ, boolean ring) {
        LevelChunkSection[] sections = chunk.getSections();
        int                 bottom   = Math.max(ring ? minY : minY + 1, chunk.getMinY());
        int                 top      = Math.min(ring ? maxY : maxY - 1, chunk.getMaxY());

        for (int sectionY = SectionPos.blockToSectionCoord(bottom); sectionY <= SectionPos.blockToSectionCoord(top); sectionY ++) {
            LevelChunkSection section = sections[chunk.getSectionIndexFromSectionY(sectionY)];

            if (section.hasOnlyAir())
                continue;

            int fromY = Math.max(bottom, sectionY << 4);
            int toY   = Math.min(top, (sectionY << 4) + 15);

            for (int y = fromY; y <= toY; y ++) {
                int faceY = y == minY || y == maxY ? 1 : 0;

                for (int z = fromZ; z <= toZ; z ++) {
                    int faceYZ = faceY + (z == minZ || z == maxZ ? 1 : 0);

                    for (int x = fromX; x <= toX; x ++) {
                        int faceType = faceYZ + (x == minX || x == maxX ? 1 : 0);

                        if (ring && faceType == 0)
                            continue;

                        BlockState state = section.getBlockState(x & 15, y & 15, z & 15);

                        if (!state.isAir() && canCollide(state, faceType))
                            return false;
                    }
                }
            }
        }

        return true;
    }

    private static boolean canCollide(BlockState state, int faceType) {
        return switch (faceType) {
            case 0 -> true;
            case 1 -> state.hasLargeCollisionShape();
            case 2 -> state.is(Blocks.MOVING_PISTON);
            case 3 -> false;
            default -> throw new IllegalStateException("Unexpected cursor face type " + faceType);
        };
    }
}
