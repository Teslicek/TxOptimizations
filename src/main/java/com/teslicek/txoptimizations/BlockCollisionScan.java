package com.teslicek.txoptimizations;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BlockCollisionScan {

    private static final double EPSILON = 1.0E-7;

    private BlockCollisionScan() {
    }

    public static boolean cannotCollide(Level level, CollisionContext context, AABB box) {
        if (level.isDebug())
            return false;

        int                      minX = Mth.floor(box.minX - EPSILON) - 1;
        int                      maxX = Mth.floor(box.maxX + EPSILON) + 1;
        int                      minY = Mth.floor(box.minY - EPSILON) - 1;
        int                      maxY = Mth.floor(box.maxY + EPSILON) + 1;
        int                      minZ = Mth.floor(box.minZ - EPSILON) - 1;
        int                      maxZ = Mth.floor(box.maxZ + EPSILON) + 1;
        BlockPos.MutableBlockPos pos  = null;

        for (int chunkX = SectionPos.blockToSectionCoord(minX); chunkX <= SectionPos.blockToSectionCoord(maxX); chunkX ++) {
            for (int chunkZ = SectionPos.blockToSectionCoord(minZ); chunkZ <= SectionPos.blockToSectionCoord(maxZ); chunkZ ++) {
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);

                if (chunk == null)
                    continue;

                LevelChunkSection[] sections = chunk.getSections();
                int                 fromX    = Math.max(minX, SectionPos.sectionToBlockCoord(chunkX));
                int                 toX      = Math.min(maxX, SectionPos.sectionToBlockCoord(chunkX) + 15);
                int                 fromZ    = Math.max(minZ, SectionPos.sectionToBlockCoord(chunkZ));
                int                 toZ      = Math.min(maxZ, SectionPos.sectionToBlockCoord(chunkZ) + 15);
                int                 bottom   = Math.max(minY, chunk.getMinY());
                int                 top      = Math.min(maxY, chunk.getMaxY());

                for (int sectionY = SectionPos.blockToSectionCoord(bottom); sectionY <= SectionPos.blockToSectionCoord(top); sectionY ++) {
                    LevelChunkSection section = sections[chunk.getSectionIndexFromSectionY(sectionY)];

                    if (section.hasOnlyAir())
                        continue;

                    int inset    = mayReachNeighborCells(section) ? 0 : 1;
                    int fromY    = Math.max(Math.max(bottom, minY + inset), SectionPos.sectionToBlockCoord(sectionY));
                    int toY      = Math.min(Math.min(top, maxY - inset), SectionPos.sectionToBlockCoord(sectionY) + 15);
                    int insetToZ = Math.min(toZ, maxZ - inset);
                    int insetToX = Math.min(toX, maxX - inset);

                    for (int y = fromY; y <= toY; y ++) {
                        int yFaces = y == minY || y == maxY ? 1 : 0;

                        for (int z = Math.max(fromZ, minZ + inset); z <= insetToZ; z ++) {
                            int yzFaces = yFaces + (z == minZ || z == maxZ ? 1 : 0);

                            for (int x = Math.max(fromX, minX + inset); x <= insetToX; x ++) {
                                int type = yzFaces + (x == minX || x == maxX ? 1 : 0);

                                if (type == Cursor3D.TYPE_CORNER)
                                    continue;

                                BlockState state = section.getBlockState(x & 15, y & 15, z & 15);

                                if (state.isAir() || type == Cursor3D.TYPE_FACE && !state.hasLargeCollisionShape() || type == Cursor3D.TYPE_EDGE && !state.is(Blocks.MOVING_PISTON))
                                    continue;

                                if (pos == null)
                                    pos = new BlockPos.MutableBlockPos();

                                if (mayCollide(context.getCollisionShape(state, level, pos.set(x, y, z)), box, x, y, z))
                                    return false;
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    private static boolean mayReachNeighborCells(LevelChunkSection section) {
        PalettedContainer<BlockState>      states  = section.getStates();
        PalettedContainer.Data<BlockState> data    = states.data;
        Palette<BlockState>                palette = data.palette();
        int                                size    = palette.getSize();
        int                                reads   = ((PaletteReads) states).txoptimizations$reads();
        CollisionReach                     reach   = (CollisionReach) section;

        if (!reach.txoptimizations$matches(data, size, reads))
            reach.txoptimizations$remember(data, size, reads, palette.maybeHas(BlockCollisionScan::reachesNeighborCells));

        return reach.txoptimizations$reaches();
    }

    private static boolean reachesNeighborCells(BlockState state) {
        return state.hasLargeCollisionShape() || state.is(Blocks.MOVING_PISTON);
    }

    private static boolean mayCollide(VoxelShape shape, AABB box, int x, int y, int z) {
        if (shape == Shapes.block())
            return box.intersects(x, y, z, x + 1.0, y + 1.0, z + 1.0);

        return !shape.isEmpty() && shape.min(Direction.Axis.X) + x < box.maxX && shape.max(Direction.Axis.X) + x > box.minX && shape.min(Direction.Axis.Y) + y < box.maxY && shape.max(Direction.Axis.Y) + y > box.minY && shape.min(Direction.Axis.Z) + z < box.maxZ && shape.max(Direction.Axis.Z) + z > box.minZ;
    }
}
