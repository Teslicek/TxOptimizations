package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.CompactVectorArray;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import com.teslicek.txoptimizations.VertexFormatElementCache;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MeshData.class)
public abstract class MeshDataCentroidMixin {

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
            float x   = (MemoryUtil.memGetFloat(first) + MemoryUtil.memGetFloat(third)) / 2.0F;
            float y   = (MemoryUtil.memGetFloat(first + 4) + MemoryUtil.memGetFloat(third + 4)) / 2.0F;
            float z   = (MemoryUtil.memGetFloat(first + 8) + MemoryUtil.memGetFloat(third + 8)) / 2.0F;

            centroids.set(firstQuad + quad, x, y, z);
        }

        ci.cancel();
    }
}
