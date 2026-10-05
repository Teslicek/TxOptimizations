package com.teslicek.txoptimizations.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.lists.VisibleChunkCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = VisibleChunkCollector.class, remap = false)
public abstract class VisibleChunkCollectorRegionCacheMixin {

    @Unique
    private boolean txoptimizations$hasRegion;

    @Unique
    private int txoptimizations$regionX;

    @Unique
    private int txoptimizations$regionY;

    @Unique
    private int txoptimizations$regionZ;

    @Unique
    private RenderRegion txoptimizations$region;

    @Redirect(method = "visit", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/region/RenderRegionManager;getForChunk(III)Lnet/caffeinemc/mods/sodium/client/render/chunk/region/RenderRegion;"))
    private RenderRegion txoptimizations$lastRegion(RenderRegionManager regions, int x, int y, int z) {
        int regionX = x >> RenderRegion.REGION_WIDTH_SH;
        int regionY = y >> RenderRegion.REGION_HEIGHT_SH;
        int regionZ = z >> RenderRegion.REGION_LENGTH_SH;

        if (this.txoptimizations$hasRegion && regionX == this.txoptimizations$regionX && regionY == this.txoptimizations$regionY && regionZ == this.txoptimizations$regionZ)
            return this.txoptimizations$region;

        this.txoptimizations$region    = regions.getForChunk(x, y, z);
        this.txoptimizations$regionX   = regionX;
        this.txoptimizations$regionY   = regionY;
        this.txoptimizations$regionZ   = regionZ;
        this.txoptimizations$hasRegion = true;

        return this.txoptimizations$region;
    }
}
