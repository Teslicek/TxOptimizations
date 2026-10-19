package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.CompactVectorArray;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.teslicek.txoptimizations.SharedQuadSorter;
import com.teslicek.txoptimizations.TwinQuads;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MeshData.SortState.class)
public abstract class MeshDataSortStateIndexMixin {

    @Shadow
    @Final
    private CompactVectorArray centroids;

    @Shadow
    @Final
    private IndexType indexType;

    @Inject(method = "buildSortedIndexBuffer", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$buildSortedIndicesDirectly(ByteBufferBuilder target, VertexSorting sorting, CallbackInfoReturnable<ByteBufferBuilder.Result> cir) {
        boolean shortIndices = this.txoptimizations$shortIndices();
        int[]   quadOrder    = sorting.sort(this.centroids);
        int     bytes        = TwinQuads.indexCount(this.centroids) * this.indexType.bytes;
        long    pointer      = target.reserve(bytes);
        long    end          = TwinQuads.writeIndices(pointer, quadOrder, this.centroids, shortIndices);

        if (end != pointer + bytes)
            throw new IllegalStateException("Wrote " + (end - pointer) + " bytes of sorted indices into " + bytes + " reserved bytes");

        cir.setReturnValue(target.build());
    }

    @Inject(method = "writeSortedIndexBuffer", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$writeSortedIndicesDirectly(ByteBuffer buffer, VertexSorting sorting, CallbackInfo ci) {
        if (this.centroids instanceof TwinQuads)
            throw new IllegalStateException("Quads with shared twin vertices are only sorted into section index buffers");

        int[] quadOrder = sorting instanceof SharedQuadSorter shared ? shared.txoptimizations$sortShared(this.centroids) : sorting.sort(this.centroids);

        ci.cancel();

        boolean shortIndices = this.txoptimizations$shortIndices();

        if (buffer.isDirect() && buffer.order() == ByteOrder.nativeOrder()) {
            int bytes = TwinQuads.indexCount(this.centroids) * this.indexType.bytes;

            if (bytes > buffer.remaining())
                throw new BufferOverflowException();

            TwinQuads.writeIndices(MemoryUtil.memAddress(buffer), quadOrder, this.centroids, shortIndices);
            buffer.position(buffer.position() + bytes);
            return;
        }

        if (shortIndices) {
            ShortBuffer indices = buffer.asShortBuffer();

            for (int quad : quadOrder) {
                int first = quad * 4;

                indices.put((short) first);
                indices.put((short) (first + 1));
                indices.put((short) (first + 2));
                indices.put((short) (first + 2));
                indices.put((short) (first + 3));
                indices.put((short) first);
            }

            buffer.position(buffer.position() + indices.position() * Short.BYTES);
            return;
        }

        IntBuffer indices = buffer.asIntBuffer();

        for (int quad : quadOrder) {
            int first = quad * 4;

            indices.put(first);
            indices.put(first + 1);
            indices.put(first + 2);
            indices.put(first + 2);
            indices.put(first + 3);
            indices.put(first);
        }

        buffer.position(buffer.position() + indices.position() * Integer.BYTES);
    }

    @Unique
    private boolean txoptimizations$shortIndices() {
        if (this.indexType != IndexType.SHORT && this.indexType != IndexType.INT)
            throw new IllegalStateException("Unknown index type " + this.indexType);

        return this.indexType == IndexType.SHORT;
    }
}
