package com.teslicek.txoptimizations.mixin.cull;

import com.teslicek.txoptimizations.cull.TerrainCuller;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSectionManager.class)
public abstract class RenderSectionManagerCullMixin {

    @Shadow
    public abstract SortedRenderLists getRenderLists();

    @Inject(method = "prepareChunkRendering", at = @At("HEAD"))
    private void txoptimizations$cullHiddenSections(ChunkRenderMatrices matrices, double x, double y, double z, boolean indexedRenderingEnabled, CallbackInfo ci) {
        TerrainCuller.cull((RenderSectionManagerInvoker) this, this.getRenderLists(), matrices, x, y, z);
    }
}
