package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teslicek.txoptimizations.MergedDrawBatch;
import net.caffeinemc.mods.sodium.client.gpu.device.batch.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.SharedQuadIndexBuffer;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DefaultChunkRenderer.class)
public abstract class DefaultChunkRendererDrawMergeMixin {

    @Shadow
    @Final
    private SharedQuadIndexBuffer sharedIndexBuffer;

    @WrapOperation(method = "prepare", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/DefaultChunkRenderer;fillCommandBuffer(Lnet/caffeinemc/mods/sodium/client/gpu/device/batch/MultiDrawBatch;Lnet/caffeinemc/mods/sodium/client/render/chunk/region/RenderRegion;Lnet/caffeinemc/mods/sodium/client/render/chunk/data/SectionRenderDataStorage;Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/ChunkRenderList;Lnet/caffeinemc/mods/sodium/client/render/viewport/CameraTransform;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;ZZ)V"))
    private void txoptimizations$mergeContiguousDraws(MultiDrawBatch batch, RenderRegion region, SectionRenderDataStorage storage, ChunkRenderList renderList, CameraTransform camera, TerrainRenderPass pass, boolean useBlockFaceCulling, boolean useIndexedTessellation, Operation<Void> original) {
        original.call(batch, region, storage, renderList, camera, pass, useBlockFaceCulling, useIndexedTessellation);

        if (!(batch instanceof MergedDrawBatch merged))
            return;

        int capacity = ((SharedQuadIndexBufferAccessor) this.sharedIndexBuffer).txoptimizations$maxPrimitives() * 6;

        merged.txoptimizations$mergeContiguous(Math.max(capacity, batch.getMaxElementCount()), !useIndexedTessellation);
    }
}
