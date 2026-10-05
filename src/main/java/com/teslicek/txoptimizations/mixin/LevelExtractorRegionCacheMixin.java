package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorRegionCacheMixin {

    @Unique
    private static final RenderRegionCache UNUSED_CACHE = new RenderRegionCache();

    @Shadow
    @Final
    private LevelRenderer levelRenderer;

    @Redirect(method = "extract", at = @At(value = "NEW", target = "()Lnet/minecraft/client/renderer/chunk/RenderRegionCache;"))
    private RenderRegionCache txoptimizations$cacheOnlyWhenUsed() {
        return this.levelRenderer.visibleSections().isEmpty() ? UNUSED_CACHE : new RenderRegionCache();
    }
}
