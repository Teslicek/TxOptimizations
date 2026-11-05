package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.CompactVectorArray;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import com.teslicek.txoptimizations.DistanceSorting;
import com.teslicek.txoptimizations.TwinQuads;
import com.teslicek.txoptimizations.VertexFormatElementCache;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MeshData.class)
public abstract class MeshDataMixin {

    @Unique
    private static final int POSITION_BYTES = 3 * Float.BYTES;

    @Inject(method = "decodeQuadCentroids", at = @At("HEAD"), cancellable = true)
    private static void txoptimizations$decodeFromNativeMemory(ByteBuffer buffer, int vertexCount, VertexFormat format, CompactVectorArray centroids, int firstQuad, CallbackInfo ci) {
        if (!buffer.isDirect() || buffer.order() != ByteOrder.nativeOrder())
            return;

        VertexFormatElement position = ((VertexFormatElementCache) format).txoptimizations$getPositionElement();

        if (position == null)
            throw new IllegalArgumentException("Cannot identify quad centers with no position element");

        int start      = buffer.position() + position.offset();
        int vertexSize = format.getVertexSize();
        int quadSize   = vertexSize * 4;
        int quads      = vertexCount / 4;

        if (quads > 0) {
            long end = start + (long) (quads - 1) * quadSize + vertexSize * 2L + POSITION_BYTES;

            if (start < 0 || end > buffer.limit())
                throw new IndexOutOfBoundsException("Quad positions end at byte " + end + " but the buffer limit is " + buffer.limit());
        }

        long base = MemoryUtil.memAddress0(buffer) + start;

        for (int quad = 0; quad < quads; quad ++) {
            long first = base + (long) quad * quadSize;
            long third = first + vertexSize * 2L;

            centroids.set(firstQuad + quad, DistanceSorting.centroid(first, third), DistanceSorting.centroid(first + 4, third + 4), DistanceSorting.centroid(first + 8, third + 8));
        }

        ci.cancel();
    }

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
