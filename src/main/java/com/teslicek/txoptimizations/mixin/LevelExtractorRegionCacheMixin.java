package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorRegionCacheMixin {

    @Unique
    private static final RenderRegionCache UNUSED_CACHE = new RenderRegionCache();

    @Redirect(method = "extract", at = @At(value = "NEW", target = "()Lnet/minecraft/client/renderer/chunk/RenderRegionCache;"))
    private RenderRegionCache txoptimizations$deferCache() {
        return UNUSED_CACHE;
    }

    @Redirect(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderRegionCache;createRegion(Lnet/minecraft/client/multiplayer/ClientLevel;J)Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;"))
    private RenderSectionRegion txoptimizations$createCacheOnFirstUse(RenderRegionCache deferred, ClientLevel level, long sectionNode, @Share("regionCache") LocalRef<RenderRegionCache> cache) {
        if (deferred != UNUSED_CACHE)
            throw new IllegalStateException("Section regions were requested from a region cache that was not deferred: " + deferred);

        if (cache.get() == null)
            cache.set(new RenderRegionCache());

        return cache.get().createRegion(level, sectionNode);
    }
}
