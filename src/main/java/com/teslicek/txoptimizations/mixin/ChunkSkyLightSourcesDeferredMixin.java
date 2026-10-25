package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.DeferredSkyLightSources;
import java.util.Arrays;
import net.minecraft.core.SectionPos;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.BlockGetter;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChunkSkyLightSources.class, priority = 999)
public abstract class ChunkSkyLightSourcesDeferredMixin implements DeferredSkyLightSources {

    @Unique
    private static final int COLUMNS = 256;

    @Shadow
    @Final
    private int minY;

    @Shadow
    @Final
    private BitStorage heightmap;

    @Unique
    private ChunkAccess txoptimizations$pendingChunk;

    @Shadow
    public void fillFrom(ChunkAccess chunk) {
        throw new AssertionError();
    }

    @Shadow
    private int get(int index) {
        throw new AssertionError();
    }

    @Shadow
    private int extendSourcesBelowWorld(int value) {
        throw new AssertionError();
    }

    @Shadow
    private static int index(int x, int z) {
        throw new AssertionError();
    }

    @Shadow
    private static boolean isEdgeOccluded(BlockState topState, BlockState bottomState) {
        throw new AssertionError();
    }

    @Override
    public void txoptimizations$fillLater(ChunkAccess chunk) {
        this.txoptimizations$pendingChunk = chunk;
    }

    @Inject(method = "fillFrom", at = @At("HEAD"))
    private void txoptimizations$dropPendingFill(ChunkAccess chunk, CallbackInfo ci) {
        this.txoptimizations$pendingChunk = null;
    }

    @Inject(method = "update", at = @At("HEAD"))
    private void txoptimizations$fillBeforeUpdate(BlockGetter level, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        this.txoptimizations$fillPending();
    }

    @Overwrite
    public int getLowestSourceY(int x, int z) {
        this.txoptimizations$fillPending();

        return this.extendSourcesBelowWorld(this.get(index(x, z)));
    }

    @Overwrite
    public int getHighestLowestSourceY() {
        if (this.txoptimizations$pendingChunk != null)
            return this.extendSourcesBelowWorld(this.txoptimizations$findHighestSourceY(this.txoptimizations$pendingChunk));

        int maxValue = Integer.MIN_VALUE;

        for (int index = 0; index < this.heightmap.getSize(); index ++)
            maxValue = Math.max(maxValue, this.heightmap.get(index));

        return this.extendSourcesBelowWorld(maxValue + this.minY);
    }

    @Unique
    private void txoptimizations$fillPending() {
        ChunkAccess chunk = this.txoptimizations$pendingChunk;

        if (chunk != null)
            this.fillFrom(chunk);
    }

    @Unique
    private int txoptimizations$findHighestSourceY(ChunkAccess chunk) {
        int topSectionIndex = chunk.getHighestFilledSectionIndex();

        if (topSectionIndex == -1)
            return this.minY;

        BlockState   air  = Blocks.AIR.defaultBlockState();
        BlockState[] tops = new BlockState[COLUMNS];
        BlockState   open = null;

        Arrays.fill(tops, air);

        for (int sectionIndex = topSectionIndex; sectionIndex >= 0; sectionIndex --) {
            LevelChunkSection section = chunk.getSection(sectionIndex);

            if (section.hasOnlyAir()) {
                Arrays.fill(tops, air);
                continue;
            }

            PalettedContainer<BlockState>      states      = section.getStates();
            PalettedContainer.Data<BlockState> data        = states.data;
            BitStorage                         storage     = data.storage();
            Palette<BlockState>                palette     = data.palette();
            int                                bottomY     = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(sectionIndex));
            int                                lastId      = -1;
            BlockState                         bottomState = null;

            for (int y = 15; y >= 0; y --) {
                for (int z = 0; z < 16; z ++) {
                    for (int x = 0; x < 16; x ++) {
                        int column = index(x, z);
                        int id     = storage.get(states.strategy.getIndex(x, y, z));

                        if (id != lastId) {
                            bottomState = palette.valueFor(id);
                            lastId      = id;
                        }

                        BlockState topState = tops[column];

                        if (topState != open || bottomState != open) {
                            if (isEdgeOccluded(topState, bottomState))
                                return bottomY + y + 1;

                            if (topState == bottomState)
                                open = bottomState;
                        }

                        tops[column] = bottomState;
                    }
                }
            }
        }

        return this.minY;
    }
}
