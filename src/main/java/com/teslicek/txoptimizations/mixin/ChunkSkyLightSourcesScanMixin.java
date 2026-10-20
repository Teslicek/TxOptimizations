package com.teslicek.txoptimizations.mixin;

import net.minecraft.core.SectionPos;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ChunkSkyLightSources.class, priority = 999)
public abstract class ChunkSkyLightSourcesScanMixin {

    @Shadow
    @Final
    private int minY;

    @Shadow
    private static boolean isEdgeOccluded(BlockState topState, BlockState bottomState) {
        throw new AssertionError();
    }

    @Overwrite
    private int findLowestSourceY(ChunkAccess chunk, int topSectionIndex, int x, int z) {
        BlockState air      = Blocks.AIR.defaultBlockState();
        BlockState topState = air;
        BlockState open     = null;

        for (int sectionIndex = topSectionIndex; sectionIndex >= 0; sectionIndex --) {
            LevelChunkSection section = chunk.getSection(sectionIndex);

            if (section.hasOnlyAir()) {
                topState = air;
                continue;
            }

            PalettedContainer<BlockState>      states      = section.getStates();
            PalettedContainer.Data<BlockState> data        = states.data;
            BitStorage                         storage     = data.storage();
            Palette<BlockState>                palette     = data.palette();
            int                                topIndex    = states.strategy.getIndex(x, 15, z);
            int                                layerSize   = topIndex - states.strategy.getIndex(x, 14, z);
            int                                sectionMinY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(sectionIndex));
            int                                lastId      = -1;
            BlockState                         bottomState = null;

            for (int y = 15, index = topIndex; y >= 0; y --, index -= layerSize) {
                int id = storage.get(index);

                if (id != lastId) {
                    bottomState = palette.valueFor(id);
                    lastId      = id;
                }

                if (topState != open || bottomState != open) {
                    if (isEdgeOccluded(topState, bottomState))
                        return sectionMinY + y + 1;

                    if (topState == bottomState)
                        open = bottomState;
                }

                topState = bottomState;
            }
        }

        return this.minY;
    }
}
