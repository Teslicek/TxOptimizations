package com.teslicek.txoptimizations.mixin.cull;

import com.teslicek.txoptimizations.cull.RegionOcclusion;
import com.teslicek.txoptimizations.cull.SectionOccluders;
import com.teslicek.txoptimizations.cull.TerrainCuller;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderRegion.class)
public abstract class RenderRegionOcclusionMixin implements RegionOcclusion {

    @Unique
    private final int[][] txoptimizations$occluders = new int[RenderRegion.REGION_SIZE][];

    @Unique
    private final long[] txoptimizations$hiddenSections = new long[RenderRegion.REGION_SIZE / Long.SIZE];

    @Inject(method = "setSectionRenderState", at = @At("RETURN"))
    private void txoptimizations$storeOccluders(int sectionIndex, BuiltSectionInfo info, CallbackInfo ci) {
        this.txoptimizations$occluders[sectionIndex] = ((SectionOccluders) info).txoptimizations$getOccluders();
        TerrainCuller.markOccludersChanged();
    }

    @Inject(method = "clearSectionRenderState", at = @At("RETURN"))
    private void txoptimizations$clearOccluders(int sectionIndex, CallbackInfo ci) {
        this.txoptimizations$occluders[sectionIndex] = null;
        TerrainCuller.markOccludersChanged();
    }

    @Override
    public int[] txoptimizations$getOccluders(int sectionIndex) {
        return this.txoptimizations$occluders[sectionIndex];
    }

    @Override
    public long[] txoptimizations$getHiddenSections() {
        return this.txoptimizations$hiddenSections;
    }
}
