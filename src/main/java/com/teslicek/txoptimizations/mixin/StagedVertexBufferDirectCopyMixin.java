package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.CompactVectorArray;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.device.GpuDevice;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.List;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(StagedVertexBuffer.class)
public abstract class StagedVertexBufferDirectCopyMixin {

    @Shadow
    @Final
    private ByteBufferBuilder stagingBuffer;

    @Shadow
    private static CompactVectorArray decodeSortingPoints(StagedVertexBuffer.Draw draw) {
        throw new AssertionError();
    }

    @ModifyConstant(method = "<init>", constant = {@Constant(intValue = GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_VERTEX), @Constant(intValue = GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_INDEX)})
    private int txoptimizations$writeDirectly(int usage) {
        return usage & ~GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_WRITE;
    }

    @Overwrite
    private void uploadDrawsToBuffers(GpuDevice device, List<StagedVertexBuffer.Draw> draws, GpuBuffer vertexGpuBuffer, @Nullable GpuBuffer indexGpuBuffer, int vertexBufferSize, int indexBufferSize) {
        try (GpuBufferSlice.MappedView view = vertexGpuBuffer.slice(0L, vertexBufferSize).map(false, true)) {
            ByteBuffer buffer = view.data();

            for (StagedVertexBuffer.Draw draw : draws) {
                if (draw.isEmpty())
                    continue;

                StagedVertexBufferDrawAccessor accessor = (StagedVertexBufferDrawAccessor) draw;

                buffer.position(accessor.txoptimizations$vertexOffset());

                for (ByteBufferBuilder.Result slice : accessor.txoptimizations$vertexBufferSlices())
                    this.txoptimizations$copy(slice, buffer);
            }
        }

        if (indexGpuBuffer != null) {
            try (GpuBufferSlice.MappedView view = indexGpuBuffer.slice(0L, indexBufferSize).map(false, true)) {
                ByteBuffer buffer = view.data();

                for (StagedVertexBuffer.Draw draw : draws) {
                    StagedVertexBufferDrawAccessor accessor = (StagedVertexBufferDrawAccessor) draw;

                    if (draw.isEmpty() || accessor.txoptimizations$quadSorting() == null)
                        continue;

                    MeshData.SortState sortState = new MeshData.SortState(decodeSortingPoints(draw), accessor.txoptimizations$indexType());

                    buffer.position(accessor.txoptimizations$indexOffset());
                    sortState.writeSortedIndexBuffer(buffer, accessor.txoptimizations$quadSorting());
                }
            }
        }

        for (StagedVertexBuffer.Draw draw : draws) {
            if (!draw.isEmpty())
                ((StagedVertexBufferDrawAccessor) draw).txoptimizations$freeVertexData();
        }
    }

    @Unique
    private void txoptimizations$copy(ByteBufferBuilder.Result slice, ByteBuffer buffer) {
        ByteBufferBuilderAccessor builder = (ByteBufferBuilderAccessor) this.stagingBuffer;
        ByteBufferResultAccessor  result  = (ByteBufferResultAccessor) slice;
        int                       size    = slice.size();
        int                       start   = buffer.position();

        if (!builder.txoptimizations$isValid(result.txoptimizations$generation()))
            throw new IllegalStateException("Buffer is no longer valid");

        if (size > buffer.remaining())
            throw new BufferOverflowException();

        MemoryUtil.memCopy(builder.txoptimizations$pointer() + result.txoptimizations$offset(), MemoryUtil.memAddress(buffer), size);
        buffer.position(start + size);
    }
}
