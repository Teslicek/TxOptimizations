package com.teslicek.txoptimizations.mixin.cull;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teslicek.txoptimizations.cull.OcclusionCuller;
import net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import net.caffeinemc.mods.sodium.client.util.iterator.ByteIterator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DefaultChunkRenderer.class)
public abstract class DefaultChunkRendererCullMixin {

    @WrapOperation(method = "fillCommandBuffer", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/ChunkRenderList;sectionsWithGeometryIterator(Z)Lnet/caffeinemc/mods/sodium/client/util/iterator/ByteIterator;"))
    private static ByteIterator txoptimizations$skipHiddenSections(ChunkRenderList list, boolean reverse, Operation<ByteIterator> original) {
        return OcclusionCuller.filter(list, original.call(list, reverse));
    }
}
