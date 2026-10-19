package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SectionGridRecenter;
import com.teslicek.txoptimizations.SectionSubset;
import net.minecraft.client.RotatingSectionStorage;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RotatingSectionStorage.class)
public abstract class RotatingSectionStorageRepositionMixin<T extends RotatingSectionStorage.Value> {

    @Shadow
    @Final
    private RotatingSectionStorage.Node<T>[] nodes;

    @Shadow
    @Final
    private int radius;

    @Shadow
    @Final
    private int minY;

    @Shadow
    @Final
    private int sectionGridSizeY;

    @Shadow
    @Final
    private int sectionGridSizeXZ;

    @Shadow
    private SectionPos centerSectionPos;

    @Shadow
    protected abstract int getSectionIndex(int x, int y, int z);

    @Overwrite
    public boolean repositionCenter(SectionPos newCenterSectionPos) {
        if (newCenterSectionPos.equals(this.centerSectionPos))
            return false;

        SectionPos previous = this.centerSectionPos;
        int        lowestX  = newCenterSectionPos.x() - this.radius;
        int        lowestZ  = newCenterSectionPos.z() - this.radius;

        SectionSubset.DIRTY.beginChange();

        if (Math.abs((long) newCenterSectionPos.x() - previous.x()) < this.sectionGridSizeXZ && Math.abs((long) newCenterSectionPos.z() - previous.z()) < this.sectionGridSizeXZ)
            this.txoptimizations$moveShifted(lowestX, lowestZ, previous.x() - this.radius, previous.z() - this.radius);
        else
            this.txoptimizations$moveAll(lowestX, lowestZ);

        this.centerSectionPos = newCenterSectionPos;
        ((SectionGridRecenter) (Object) this).txoptimizations$recenterWindow();
        SectionSubset.DIRTY.endChange();

        return true;
    }

    @Unique
    private void txoptimizations$moveAll(int lowestX, int lowestZ) {
        for (int gridX = 0; gridX < this.sectionGridSizeXZ; gridX ++) {
            int sectionX = txoptimizations$sectionAt(gridX, lowestX, this.sectionGridSizeXZ);

            for (int gridZ = 0; gridZ < this.sectionGridSizeXZ; gridZ ++) {
                int sectionZ = txoptimizations$sectionAt(gridZ, lowestZ, this.sectionGridSizeXZ);

                for (int gridY = 0; gridY < this.sectionGridSizeY; gridY ++)
                    this.txoptimizations$move(this.nodes[this.getSectionIndex(gridX, gridY, gridZ)].value(), SectionPos.asLong(sectionX, this.minY + gridY, sectionZ));
            }
        }
    }

    @Unique
    private void txoptimizations$moveShifted(int lowestX, int lowestZ, int previousLowestX, int previousLowestZ) {
        int size = this.sectionGridSizeXZ;

        for (int gridX = 0; gridX < size; gridX ++) {
            int     sectionX    = txoptimizations$sectionAt(gridX, lowestX, size);
            int     previousX   = txoptimizations$sectionAt(gridX, previousLowestX, size);
            boolean columnMoved = sectionX != previousX;

            for (int gridZ = 0; gridZ < size; gridZ ++) {
                int sectionZ  = txoptimizations$sectionAt(gridZ, lowestZ, size);
                int previousZ = txoptimizations$sectionAt(gridZ, previousLowestZ, size);

                if (columnMoved || sectionZ != previousZ)
                    this.txoptimizations$moveColumn(gridX, gridZ, sectionX, sectionZ, previousX, previousZ);
            }
        }
    }

    @Unique
    private void txoptimizations$moveColumn(int gridX, int gridZ, int sectionX, int sectionZ, int previousX, int previousZ) {
        for (int gridY = 0; gridY < this.sectionGridSizeY; gridY ++) {
            int  sectionY = this.minY + gridY;
            T    value    = this.nodes[this.getSectionIndex(gridX, gridY, gridZ)].value();
            long current  = value.getSectionNode();

            if (current != SectionPos.asLong(previousX, sectionY, previousZ))
                throw new IllegalStateException("Section grid slot " + gridX + "," + gridY + "," + gridZ + " holds " + SectionPos.of(current) + " instead of " + SectionPos.of(previousX, sectionY, previousZ) + " before moving to " + SectionPos.of(sectionX, sectionY, sectionZ));

            this.txoptimizations$move(value, SectionPos.asLong(sectionX, sectionY, sectionZ));
        }
    }

    @Unique
    private void txoptimizations$move(T value, long sectionNode) {
        if (value.getSectionNode() != sectionNode)
            value.setSectionNode(sectionNode);
    }

    @Unique
    private static int txoptimizations$sectionAt(int grid, int lowest, int size) {
        return lowest + Math.floorMod(grid - lowest, size);
    }
}
