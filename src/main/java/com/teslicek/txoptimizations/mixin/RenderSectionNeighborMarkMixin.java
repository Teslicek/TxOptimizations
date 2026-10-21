package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.NeighborMark;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class RenderSectionNeighborMarkMixin implements NeighborMark {

    @Unique
    private long txoptimizations$missingNeighborsNode;

    @Unique
    private int txoptimizations$missingNeighborsGeneration;

    @Override
    public long txoptimizations$getMissingNeighborsNode() {
        return this.txoptimizations$missingNeighborsNode;
    }

    @Override
    public int txoptimizations$getMissingNeighborsGeneration() {
        return this.txoptimizations$missingNeighborsGeneration;
    }

    @Override
    public void txoptimizations$markMissingNeighbors(long sectionNode, int generation) {
        this.txoptimizations$missingNeighborsNode       = sectionNode;
        this.txoptimizations$missingNeighborsGeneration = generation;
    }
}
