package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.CompactVectorArray;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.renderpearl.api.pipeline.IndexType;
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

@Mixin(MeshData.SortState.class)
public abstract class MeshDataSortStateIndexMixin {

    @Unique
    private static final int QUAD_INDICES = 6;

    @Shadow
    @Final
    private CompactVectorArray centroids;

    @Shadow
    @Final
    private IndexType indexType;

    @Inject(method = "writeSortedIndexBuffer", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$writeSortedIndicesDirectly(ByteBuffer buffer, VertexSorting sorting, CallbackInfo ci) {
        int[] quadOrder = sorting.sort(this.centroids);

        ci.cancel();

        if (this.indexType != IndexType.SHORT && this.indexType != IndexType.INT)
            throw new IllegalStateException("Unknown index type " + this.indexType);

        boolean shortIndices = this.indexType == IndexType.SHORT;

        if (buffer.isDirect() && buffer.order() == ByteOrder.nativeOrder()) {
            txoptimizations$writeToMemory(buffer, quadOrder, shortIndices);
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
    private static void txoptimizations$writeToMemory(ByteBuffer buffer, int[] quadOrder, boolean shortIndices) {
        int indexBytes = shortIndices ? Short.BYTES : Integer.BYTES;
        int bytes      = quadOrder.length * QUAD_INDICES * indexBytes;

        if (bytes > buffer.remaining())
            throw new BufferOverflowException();

        long pointer = MemoryUtil.memAddress(buffer);

        for (int quad : quadOrder) {
            int first = quad * 4;

            if (shortIndices) {
                MemoryUtil.memPutShort(pointer, (short) first);
                MemoryUtil.memPutShort(pointer + 2L, (short) (first + 1));
                MemoryUtil.memPutShort(pointer + 4L, (short) (first + 2));
                MemoryUtil.memPutShort(pointer + 6L, (short) (first + 2));
                MemoryUtil.memPutShort(pointer + 8L, (short) (first + 3));
                MemoryUtil.memPutShort(pointer + 10L, (short) first);
            } else {
                MemoryUtil.memPutInt(pointer, first);
                MemoryUtil.memPutInt(pointer + 4L, first + 1);
                MemoryUtil.memPutInt(pointer + 8L, first + 2);
                MemoryUtil.memPutInt(pointer + 12L, first + 2);
                MemoryUtil.memPutInt(pointer + 16L, first + 3);
                MemoryUtil.memPutInt(pointer + 20L, first);
            }

            pointer += QUAD_INDICES * indexBytes;
        }

        buffer.position(buffer.position() + bytes);
    }
}
