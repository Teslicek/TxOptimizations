package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.CompactVectorArray;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.teslicek.txoptimizations.BackgroundVertexCopy;
import com.teslicek.txoptimizations.OutlineDraw;
import com.teslicek.txoptimizations.PositionBounds;
import com.teslicek.txoptimizations.VertexCopy;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StagedVertexBuffer.class, priority = 999)
public abstract class StagedVertexBufferDirectCopyMixin implements BackgroundVertexCopy {

    @Shadow
    @Final
    private ByteBufferBuilder stagingBuffer;

    @Shadow
    @Final
    private List<StagedVertexBuffer.Draw> draws;

    @Unique
    private final VertexCopy txoptimizations$vertexCopy = new VertexCopy();

    @Unique
    private GpuBufferSlice.MappedView txoptimizations$vertexView;

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
        if (this.txoptimizations$vertexView != null)
            throw new IllegalStateException("The previous vertex upload has not finished");

        this.txoptimizations$vertexView = vertexGpuBuffer.slice(0L, vertexBufferSize).map(false, true);

        ByteBuffer vertices = this.txoptimizations$vertexView.data();

        this.txoptimizations$vertexCopy.begin();

        for (StagedVertexBuffer.Draw draw : draws) {
            if (draw.isEmpty())
                continue;

            StagedVertexBufferDrawAccessor accessor = (StagedVertexBufferDrawAccessor) draw;

            vertices.position(accessor.txoptimizations$vertexOffset());

            for (ByteBufferBuilder.Result slice : accessor.txoptimizations$vertexBufferSlices())
                this.txoptimizations$queueCopy(slice, vertices);

            if (((OutlineDraw) draw).txoptimizations$isOutlineTracked())
                this.txoptimizations$queueOutlineBounds((OutlineDraw) draw, accessor.txoptimizations$vertexBufferSlices());
        }

        this.txoptimizations$vertexCopy.seal();

        if (indexGpuBuffer == null)
            return;

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

    @Override
    public void txoptimizations$copyVerticesInBackground() {
        this.txoptimizations$vertexCopy.startInBackground();
    }

    @Inject(method = "getExecuteInfo", at = @At("HEAD"))
    private void txoptimizations$finishBeforeDraw(StagedVertexBuffer.Draw draw, CallbackInfoReturnable<StagedVertexBuffer.ExecuteInfo> cir) {
        this.txoptimizations$finishVertexCopy();
    }

    @Inject(method = {"endDraw", "close"}, at = @At("HEAD"))
    private void txoptimizations$finishBeforeRelease(CallbackInfo ci) {
        this.txoptimizations$finishVertexCopy();
    }

    @Unique
    private void txoptimizations$queueCopy(ByteBufferBuilder.Result slice, ByteBuffer buffer) {
        ByteBufferBuilderAccessor builder = (ByteBufferBuilderAccessor) this.stagingBuffer;
        ByteBufferResultAccessor  result  = (ByteBufferResultAccessor) slice;
        int                       size    = slice.size();
        int                       start   = buffer.position();

        if (!builder.txoptimizations$isValid(result.txoptimizations$generation()))
            throw new IllegalStateException("Buffer is no longer valid");

        if (size > buffer.remaining())
            throw new BufferOverflowException();

        this.txoptimizations$vertexCopy.add(builder.txoptimizations$pointer() + result.txoptimizations$offset(), MemoryUtil.memAddress(buffer), size);
        buffer.position(start + size);
    }

    @Unique
    private void txoptimizations$queueOutlineBounds(OutlineDraw draw, List<ByteBufferBuilder.Result> slices) {
        ByteBufferBuilderAccessor builder = (ByteBufferBuilderAccessor) this.stagingBuffer;
        int                       stride  = DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize();
        float[]                   boxes   = new float[slices.size() * PositionBounds.VALUES];

        draw.txoptimizations$setOutlineBoxes(boxes);

        for (int index = 0; index < slices.size(); index ++) {
            ByteBufferBuilder.Result slice = slices.get(index);

            if (slice.size() % stride != 0)
                throw new IllegalStateException("Outline slice of " + slice.size() + " bytes does not hold whole vertices of " + stride + " bytes");

            this.txoptimizations$vertexCopy.addBounds(builder.txoptimizations$pointer() + ((ByteBufferResultAccessor) slice).txoptimizations$offset(), slice.size() / stride, stride, boxes, index * PositionBounds.VALUES);
        }
    }

    @Override
    public void txoptimizations$finishVertexCopy() {
        if (this.txoptimizations$vertexView == null)
            return;

        this.txoptimizations$vertexCopy.finish();
        this.txoptimizations$vertexView.close();
        this.txoptimizations$vertexView = null;

        for (StagedVertexBuffer.Draw draw : this.draws) {
            if (!draw.isEmpty())
                ((StagedVertexBufferDrawAccessor) draw).txoptimizations$freeVertexData();
        }
    }
}
