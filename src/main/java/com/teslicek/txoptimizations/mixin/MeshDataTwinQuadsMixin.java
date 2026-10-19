package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.teslicek.txoptimizations.TwinQuads;
import java.nio.ByteBuffer;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MeshData.class)
public abstract class MeshDataTwinQuadsMixin {

    @Shadow
    @Final
    private ByteBufferBuilder.Result vertexBuffer;

    @Shadow
    private ByteBufferBuilder.@Nullable Result indexBuffer;

    @Shadow
    @Final
    @Mutable
    private MeshData.DrawState drawState;

    @Inject(method = "sortQuads", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$shareTwinVertices(ByteBufferBuilder indexBufferTarget, VertexSorting sorting, CallbackInfoReturnable<MeshData.SortState> cir) {
        MeshData.DrawState state = this.drawState;

        if (state.primitiveTopology() != PrimitiveTopology.QUADS)
            return;

        VertexFormat format     = state.format();
        int          vertexSize = format.getVertexSize();
        int          perQuad    = PrimitiveTopology.QUADS.primitiveLength;
        int          quads      = state.vertexCount() / perQuad;
        ByteBuffer   vertices   = this.vertexBuffer.byteBuffer();

        if (quads * perQuad != state.vertexCount() || (long) quads * perQuad * vertexSize != vertices.remaining())
            throw new IllegalStateException(state.vertexCount() + " vertices of " + vertexSize + " bytes do not form whole quads in " + vertices.remaining() + " bytes");

        long[] twins = TwinQuads.twinWords(quads);
        int    kept  = TwinQuads.share(MemoryUtil.memAddress(vertices), vertexSize, quads, twins);

        if (kept == quads)
            return;

        TwinQuads centroids = new TwinQuads(kept, twins);

        if (TwinQuads.indexCount(centroids) != state.indexCount())
            throw new IllegalStateException(kept + " quads and their twins make " + TwinQuads.indexCount(centroids) + " indices instead of the mesh's " + state.indexCount());

        ((ByteBufferResultAccessor) this.vertexBuffer).txoptimizations$setSize(kept * perQuad * vertexSize);
        this.drawState = new MeshData.DrawState(format, kept * perQuad, state.indexCount(), state.primitiveTopology(), state.indexType());

        MeshData.decodeQuadCentroids(this.vertexBuffer.byteBuffer(), kept * perQuad, format, centroids, 0);

        MeshData.SortState sortState = new MeshData.SortState(centroids, state.indexType());

        this.indexBuffer = sortState.buildSortedIndexBuffer(indexBufferTarget, sorting);
        cir.setReturnValue(sortState);
    }
}
