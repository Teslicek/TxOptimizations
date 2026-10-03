package com.teslicek.txoptimizations;

import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;

public final class AirCollision {

    private static final double EPSILON = 1.0E-7;

    private AirCollision() {
    }

    public static boolean isOnlyAir(Level level, AABB box) {
        int minX = Mth.floor(box.minX - EPSILON) - 1;
        int maxX = Mth.floor(box.maxX + EPSILON) + 1;
        int minY = Mth.floor(box.minY - EPSILON) - 1;
        int maxY = Mth.floor(box.maxY + EPSILON) + 1;
        int minZ = Mth.floor(box.minZ - EPSILON) - 1;
        int maxZ = Mth.floor(box.maxZ + EPSILON) + 1;

        for (int chunkX = SectionPos.blockToSectionCoord(minX); chunkX <= SectionPos.blockToSectionCoord(maxX); chunkX ++) {
            for (int chunkZ = SectionPos.blockToSectionCoord(minZ); chunkZ <= SectionPos.blockToSectionCoord(maxZ); chunkZ ++) {
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);

                if (chunk != null && !isChunkAir(chunk, Math.max(minX, chunkX << 4), Math.min(maxX, (chunkX << 4) + 15), minY, maxY, Math.max(minZ, chunkZ << 4), Math.min(maxZ, (chunkZ << 4) + 15)))
                    return false;
            }
        }

        return true;
    }

    private static boolean isChunkAir(ChunkAccess chunk, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        LevelChunkSection[] sections = chunk.getSections();
        int                 bottom   = Math.max(minY, chunk.getMinY());
        int                 top      = Math.min(maxY, chunk.getMaxY());

        for (int sectionY = SectionPos.blockToSectionCoord(bottom); sectionY <= SectionPos.blockToSectionCoord(top); sectionY ++) {
            LevelChunkSection section = sections[chunk.getSectionIndexFromSectionY(sectionY)];

            if (section.hasOnlyAir())
                continue;

            int fromY = Math.max(bottom, sectionY << 4);
            int toY   = Math.min(top, (sectionY << 4) + 15);

            for (int y = fromY; y <= toY; y ++) {
                for (int z = minZ; z <= maxZ; z ++) {
                    for (int x = minX; x <= maxX; x ++) {
                        if (!section.getBlockState(x & 15, y & 15, z & 15).isAir())
                            return false;
                    }
                }
            }
        }

        return true;
    }
}
