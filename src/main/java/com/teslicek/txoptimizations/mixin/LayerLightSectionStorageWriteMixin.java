package com.teslicek.txoptimizations.mixin;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = LayerLightSectionStorage.class, priority = 999)
public abstract class LayerLightSectionStorageWriteMixin {

    @Shadow
    @Final
    protected DataLayerStorageMap<?> updatingSectionData;

    @Shadow
    @Final
    protected LongSet changedSections;

    @Shadow
    @Final
    protected LongSet sectionsAffectedByLightUpdates;

    @Unique
    private long txoptimizations$lastChanged;

    @Unique
    private boolean txoptimizations$changedKnown;

    @Unique
    private long txoptimizations$lastAffected;

    @Unique
    private boolean txoptimizations$affectedKnown;

    @Shadow
    protected abstract DataLayer getDataLayer(long sectionNode, boolean updating);

    @Overwrite
    protected void setStoredLevel(long blockNode, int level) {
        long      sectionNode = SectionPos.blockToSection(blockNode);
        DataLayer layer       = this.txoptimizations$addChanged(sectionNode) ? this.updatingSectionData.copyDataLayer(sectionNode) : this.getDataLayer(sectionNode, true);

        layer.set(SectionPos.sectionRelative(BlockPos.getX(blockNode)), SectionPos.sectionRelative(BlockPos.getY(blockNode)), SectionPos.sectionRelative(BlockPos.getZ(blockNode)), level);
        this.txoptimizations$addAffected(blockNode, sectionNode);
    }

    @Redirect(method = "swapSectionMap", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSet;clear()V"))
    private void txoptimizations$clear(LongSet sections) {
        sections.clear();
        this.txoptimizations$changedKnown  = false;
        this.txoptimizations$affectedKnown = false;
    }

    @Unique
    private boolean txoptimizations$addChanged(long sectionNode) {
        if (this.txoptimizations$changedKnown && sectionNode == this.txoptimizations$lastChanged)
            return false;

        this.txoptimizations$lastChanged  = sectionNode;
        this.txoptimizations$changedKnown = true;

        return this.changedSections.add(sectionNode);
    }

    @Unique
    private void txoptimizations$addAffected(long blockNode, long sectionNode) {
        int x    = BlockPos.getX(blockNode);
        int y    = BlockPos.getY(blockNode);
        int z    = BlockPos.getZ(blockNode);
        int minX = SectionPos.blockToSectionCoord(x - 1);
        int maxX = SectionPos.blockToSectionCoord(x + 1);
        int minY = SectionPos.blockToSectionCoord(y - 1);
        int maxY = SectionPos.blockToSectionCoord(y + 1);
        int minZ = SectionPos.blockToSectionCoord(z - 1);
        int maxZ = SectionPos.blockToSectionCoord(z + 1);

        if (minX == maxX && minY == maxY && minZ == maxZ) {
            if (this.txoptimizations$affectedKnown && sectionNode == this.txoptimizations$lastAffected)
                return;

            this.sectionsAffectedByLightUpdates.add(sectionNode);
        } else {
            for (int sectionX = minX; sectionX <= maxX; sectionX++) {
                for (int sectionY = minY; sectionY <= maxY; sectionY++) {
                    for (int sectionZ = minZ; sectionZ <= maxZ; sectionZ++)
                        this.sectionsAffectedByLightUpdates.add(SectionPos.asLong(sectionX, sectionY, sectionZ));
                }
            }
        }

        this.txoptimizations$lastAffected  = sectionNode;
        this.txoptimizations$affectedKnown = true;
    }
}
