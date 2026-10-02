package com.teslicek.txoptimizations.mixin.cull;

import com.teslicek.txoptimizations.cull.RegionOcclusion;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RenderRegion.class)
public abstract class RenderRegionOcclusionMixin implements RegionOcclusion {

    @Unique
    private final long[] txoptimizations$hiddenSections = new long[RenderRegion.REGION_SIZE / Long.SIZE];

    @Override
    public long[] txoptimizations$getHiddenSections() {
        return this.txoptimizations$hiddenSections;
    }
}
