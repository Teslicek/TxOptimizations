package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.CompactVectorArray;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import com.teslicek.txoptimizations.BackgroundVertexCopy;
import com.teslicek.txoptimizations.DistanceSorting;
import com.teslicek.txoptimizations.InFlightVertexCopy;
import com.teslicek.txoptimizations.OutlineDraw;
import com.teslicek.txoptimizations.PositionBounds;
import com.teslicek.txoptimizations.QuadSort;
import com.teslicek.txoptimizations.TwinQuads;
import com.teslicek.txoptimizations.VertexCopy;
import com.teslicek.txoptimizations.VertexFormatElementCache;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StagedVertexBuffer.class, priority = 999)
public abstract class StagedVertexBufferDirectCopyMixin implements BackgroundVertexCopy {

    @Unique
    private static final int VERTICES_PER_QUAD = PrimitiveTopology.QUADS.primitiveLength;

    @Unique
    private static final int INDICES_PER_QUAD = PrimitiveTopology.QUADS.indexCount(VERTICES_PER_QUAD);

    @Shadow
    @Final
    private ByteBufferBuilder stagingBuffer;

    @Shadow
    @Final
    private List<StagedVertexBuffer.Draw> draws;

    @Unique
    private final VertexCopy txoptimizations$vertexCopy = new VertexCopy();

    @Unique
    private final List<StagedVertexBuffer.Draw> txoptimizations$copiedDraws = new ArrayList<>();

    @Unique
    private GpuBufferSlice.MappedView txoptimizations$vertexView;

    @Shadow
    private static CompactVectorArray decodeSortingPoints(StagedVertexBuffer.Draw draw) {
        throw new AssertionError();
    }

    @Shadow
    protected abstract void finishLastVertexBuilder();

    @ModifyConstant(method = "<init>", constant = {@Constant(intValue = GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_VERTEX), @Constant(intValue = GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_INDEX)})
    private int txoptimizations$writeDirectly(int usage) {
        return usage & ~GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_WRITE;
    }

    @Overwrite
    private void uploadDrawsToBuffers(GpuDevice device, List<StagedVertexBuffer.Draw> draws, GpuBuffer vertexGpuBuffer, @Nullable GpuBuffer indexGpuBuffer, int vertexBufferSize, int indexBufferSize) {
        if (this.txoptimizations$vertexView == null) {
            this.txoptimizations$vertexView = vertexGpuBuffer.slice(0L, vertexBufferSize).map(false, true);
            this.txoptimizations$vertexCopy.begin();
            this.txoptimizations$copyDraws(draws, this.txoptimizations$vertexView.data(), false);
            this.txoptimizations$vertexCopy.seal();
            this.txoptimizations$writeIndices(draws, indexGpuBuffer, indexBufferSize);

            return;
        }

        if (!this.txoptimizations$vertexCopy.isInBackground())
            throw new IllegalStateException("The previous vertex upload has not finished");

        try (GpuBufferSlice.MappedView view = vertexGpuBuffer.slice(0L, vertexBufferSize).map(false, true)) {
            this.txoptimizations$copyDraws(draws, view.data(), true);
        }

        this.txoptimizations$writeIndices(draws, indexGpuBuffer, indexBufferSize);

        for (StagedVertexBuffer.Draw draw : draws)
            ((StagedVertexBufferDrawAccessor) draw).txoptimizations$freeVertexData();
    }

    @Unique
    private void txoptimizations$copyDraws(List<StagedVertexBuffer.Draw> draws, ByteBuffer vertices, boolean now) {
        for (StagedVertexBuffer.Draw draw : draws) {
            if (draw.isEmpty())
                continue;

            StagedVertexBufferDrawAccessor accessor = (StagedVertexBufferDrawAccessor) draw;

            if (!now)
                this.txoptimizations$copiedDraws.add(draw);

            vertices.position(accessor.txoptimizations$vertexOffset());

            for (ByteBufferBuilder.Result slice : accessor.txoptimizations$vertexBufferSlices())
                this.txoptimizations$copy(slice, vertices, now);

            if (((OutlineDraw) draw).txoptimizations$isOutlineTracked())
                this.txoptimizations$measureOutlineBounds((OutlineDraw) draw, accessor.txoptimizations$vertexBufferSlices(), now);
        }
    }

    @Unique
    private void txoptimizations$writeIndices(List<StagedVertexBuffer.Draw> draws, @Nullable GpuBuffer indexGpuBuffer, int indexBufferSize) {
        if (indexGpuBuffer == null)
            return;

        try (GpuBufferSlice.MappedView view = indexGpuBuffer.slice(0L, indexBufferSize).map(false, true)) {
            ByteBuffer buffer = view.data();

            for (StagedVertexBuffer.Draw draw : draws) {
                StagedVertexBufferDrawAccessor accessor = (StagedVertexBufferDrawAccessor) draw;

                if (draw.isEmpty() || accessor.txoptimizations$quadSorting() == null)
                    continue;

                buffer.position(accessor.txoptimizations$indexOffset());

                if (accessor.txoptimizations$quadSorting() == VertexSorting.DISTANCE_TO_ORIGIN) {
                    this.txoptimizations$writeOriginSortedIndices(accessor, buffer);
                    continue;
                }

                MeshData.SortState sortState = new MeshData.SortState(decodeSortingPoints(draw), accessor.txoptimizations$indexType());

                sortState.writeSortedIndexBuffer(buffer, accessor.txoptimizations$quadSorting());
            }
        }
    }

    @Override
    public void txoptimizations$copyVerticesInBackground() {
        if (this.txoptimizations$vertexCopy.isInBackground())
            return;

        this.txoptimizations$vertexCopy.startInBackground();

        if (this.txoptimizations$vertexCopy.isInBackground())
            InFlightVertexCopy.set(this, this.stagingBuffer);
    }

    @ModifyVariable(method = "getExecuteInfo", at = @At("HEAD"), argsOnly = true)
    private StagedVertexBuffer.Draw txoptimizations$finishBeforeDraw(StagedVertexBuffer.Draw draw) {
        this.txoptimizations$finishUnlessInBackground();

        return draw;
    }

    @Inject(method = "endDraw", at = @At("HEAD"))
    private void txoptimizations$finishBeforeRelease(CallbackInfo ci) {
        this.txoptimizations$finishUnlessInBackground();
    }

    @Redirect(method = "getVertexBuilder", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/StagedVertexBuffer;finishLastVertexBuilder()V"))
    private void txoptimizations$finishBeforeReuse(StagedVertexBuffer buffer) {
        this.txoptimizations$finishUnlessInBackground();
        this.finishLastVertexBuilder();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void txoptimizations$finishBeforeClose(CallbackInfo ci) {
        this.txoptimizations$finishVertexCopy();
    }

    @Unique
    private void txoptimizations$writeOriginSortedIndices(StagedVertexBufferDrawAccessor draw, ByteBuffer buffer) {
        VertexFormat        format   = draw.txoptimizations$format();
        VertexFormatElement position = ((VertexFormatElementCache) format).txoptimizations$getPositionElement();

        if (position == null)
            throw new IllegalArgumentException("Cannot identify quad centers with no position element");

        if (!buffer.isDirect() || buffer.order() != ByteOrder.nativeOrder())
            throw new IllegalStateException("Sorted indices are written straight into a mapped index buffer, but got " + buffer);

        int     vertexSize   = format.getVertexSize();
        int     quads        = draw.txoptimizations$vertexCount() / VERTICES_PER_QUAD;
        boolean shortIndices = TwinQuads.shortIndices(draw.txoptimizations$indexType());
        int     bytes        = quads * INDICES_PER_QUAD * (shortIndices ? Short.BYTES : Integer.BYTES);
        int[]   keys         = QuadSort.keys(quads);
        int     quad         = 0;

        for (ByteBufferBuilder.Result slice : draw.txoptimizations$vertexBufferSlices()) {
            int sliceQuads = slice.size() / vertexSize / VERTICES_PER_QUAD;

            if (quad + sliceQuads > quads)
                throw new IndexOutOfBoundsException("Sorting points of " + (quad + sliceQuads) + " quads do not fit the " + quads + " quads of the draw");

            quad = DistanceSorting.fillOriginKeys(this.txoptimizations$address(slice) + position.offset(), vertexSize, sliceQuads, keys, quad);
        }

        Arrays.fill(keys, quad, quads, DistanceSorting.originKey(0.0F, 0.0F, 0.0F));

        if (bytes > buffer.remaining())
            throw new BufferOverflowException();

        TwinQuads.writeIndices(MemoryUtil.memAddress(buffer), QuadSort.sort(keys, quads), shortIndices);
        buffer.position(buffer.position() + bytes);
    }

    @Unique
    private void txoptimizations$finishUnlessInBackground() {
        if (this.txoptimizations$vertexView != null && !this.txoptimizations$vertexCopy.isInBackground())
            this.txoptimizations$finishVertexCopy();
    }

    @Unique
    private void txoptimizations$copy(ByteBufferBuilder.Result slice, ByteBuffer buffer, boolean now) {
        int  size   = slice.size();
        int  start  = buffer.position();
        long source = this.txoptimizations$address(slice);

        if (size > buffer.remaining())
            throw new BufferOverflowException();

        if (now)
            MemoryUtil.memCopy(source, MemoryUtil.memAddress(buffer), size);
        else
            this.txoptimizations$vertexCopy.add(source, MemoryUtil.memAddress(buffer), size);

        buffer.position(start + size);
    }

    @Unique
    private long txoptimizations$address(ByteBufferBuilder.Result slice) {
        ByteBufferBuilderAccessor builder = (ByteBufferBuilderAccessor) this.stagingBuffer;
        ByteBufferResultAccessor  result  = (ByteBufferResultAccessor) slice;

        if (!builder.txoptimizations$isValid(result.txoptimizations$generation()))
            throw new IllegalStateException("Buffer is no longer valid");

        return builder.txoptimizations$pointer() + result.txoptimizations$offset();
    }

    @Unique
    private void txoptimizations$measureOutlineBounds(OutlineDraw draw, List<ByteBufferBuilder.Result> slices, boolean now) {
        int     stride = DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize();
        float[] boxes  = new float[slices.size() * PositionBounds.VALUES];

        draw.txoptimizations$setOutlineBoxes(boxes);

        for (int index = 0; index < slices.size(); index ++) {
            ByteBufferBuilder.Result slice = slices.get(index);

            if (slice.size() % stride != 0)
                throw new IllegalStateException("Outline slice of " + slice.size() + " bytes does not hold whole vertices of " + stride + " bytes");

            if (now)
                PositionBounds.measure(this.txoptimizations$address(slice), slice.size() / stride, stride, boxes, index * PositionBounds.VALUES);
            else
                this.txoptimizations$vertexCopy.addBounds(this.txoptimizations$address(slice), slice.size() / stride, stride, boxes, index * PositionBounds.VALUES);
        }
    }

    @Override
    public void txoptimizations$finishOutlineBounds() {
        this.txoptimizations$vertexCopy.finishBounds();
    }

    @Override
    public void txoptimizations$awaitVertexCopy() {
        this.txoptimizations$vertexCopy.await();
    }

    @Override
    public void txoptimizations$finishVertexCopy() {
        if (this.txoptimizations$vertexView == null)
            return;

        this.txoptimizations$vertexCopy.finish();
        this.txoptimizations$vertexView.close();
        this.txoptimizations$vertexView = null;
        InFlightVertexCopy.clear(this);

        for (StagedVertexBuffer.Draw draw : this.txoptimizations$copiedDraws)
            ((StagedVertexBufferDrawAccessor) draw).txoptimizations$freeVertexData();

        this.txoptimizations$copiedDraws.clear();
    }
}
