package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.teslicek.txoptimizations.mixin.ByteBufferBuilderAccessor;
import com.teslicek.txoptimizations.mixin.ByteBufferResultAccessor;
import com.teslicek.txoptimizations.mixin.StagedVertexBufferDrawAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

public final class GpuCubeBatch implements AutoCloseable {

    private static final int      INITIAL_POSES = 256;
    private static final int      INITIAL_REFS  = 1024;
    private static final int      QUAD_INDICES  = 6;
    private static final int      FACES         = CubeVertices.FACES;
    private static final long     ALIGNMENT     = 256L;
    private static final int      STRIDE        = VertexBytes.ENTITY_STRIDE;
    private static final Matrix4f MATRIX        = new Matrix4f();
    private static final int[]    NORMALS       = new int[FACES];

    private final String            label;
    private final GpuBufferRing     texels;
    private final GpuBufferRing     sorted;
    private final List<GpuCubeDraw> sortedDraws = new ArrayList<>();

    private long                poses        = MemoryUtil.nmemAlloc((long) INITIAL_POSES * GpuCubes.POSE_BYTES);
    private int                 poseCapacity = INITIAL_POSES;
    private int                 poseCount;
    private long                refs         = MemoryUtil.nmemAlloc((long) INITIAL_REFS * GpuCubes.REF_BYTES);
    private int                 refCapacity  = INITIAL_REFS;
    private int                 refCount;
    private int                 longestRun;
    private @Nullable GpuBuffer current;
    private @Nullable GpuBuffer sortedBuffer;
    private long                half;

    public GpuCubeBatch(String label) {
        if (this.poses == MemoryUtil.NULL || this.refs == MemoryUtil.NULL)
            throw new OutOfMemoryError("Could not allocate GPU cube records for " + label);

        this.label  = label;
        this.texels = new GpuBufferRing("TxOptimizations GPU cubes of " + label, GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER | GpuBuffer.USAGE_MAP_WRITE);
        this.sorted = new GpuBufferRing("TxOptimizations sorted GPU cubes of " + label, GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE);
    }

    public int pose(PoseStack.Pose pose, int light, int overlay, int color) {
        if (this.poseCount == this.poseCapacity) {
            this.poseCapacity *= 2;
            this.poses         = reallocate(this.poses, (long) this.poseCapacity * GpuCubes.POSE_BYTES);
        }

        GpuCubes.writePose(this.poses + (long) this.poseCount * GpuCubes.POSE_BYTES, pose, light, overlay, color);

        return this.poseCount ++;
    }

    public int addRef(int cube, int pose) {
        if (this.refCount == this.refCapacity) {
            this.refCapacity *= 2;
            this.refs         = reallocate(this.refs, (long) this.refCapacity * GpuCubes.REF_BYTES);
        }

        long at = this.refs + (long) this.refCount * GpuCubes.REF_BYTES;

        MemoryUtil.memPutInt(at, cube);
        MemoryUtil.memPutInt(at + 4L, pose);

        return this.refCount ++;
    }

    public void noteRun(int cubes) {
        this.longestRun = Math.max(this.longestRun, cubes);
    }

    public void addSortedDraw(GpuCubeDraw draw) {
        this.sortedDraws.add(draw);
    }

    public void upload(GpuDevice device) {
        if (this.current != null || this.refCount == 0)
            return;

        long poseBytes = (long) this.poseCount * GpuCubes.POSE_BYTES;
        long refBytes  = (long) this.refCount * GpuCubes.REF_BYTES;

        this.current = this.texels.acquire(device, 2L * Math.max(poseBytes, refBytes));
        this.half    = this.current.size() / 2L;

        try (GpuBufferSlice.MappedView view = this.current.slice(0L, this.half + poseBytes).map(false, true)) {
            long target = MemoryUtil.memAddress(view.data());

            MemoryUtil.memCopy(this.refs, target, refBytes);
            MemoryUtil.memCopy(this.poses, target + this.half, poseBytes);
        }

        GpuCubes.uploadStatic(device);
        GpuCubes.requestIndices(this.longestRun);

        if (!this.sortedDraws.isEmpty())
            this.uploadSorted(device);
    }

    public void bind(RenderPass renderPass) {
        GpuBuffer buffer = this.current;

        if (buffer == null)
            throw new IllegalStateException("GPU cubes of " + this.label + " are drawn before they were uploaded");

        GpuBufferSlice poseSlice = buffer.slice(this.half, this.half);

        renderPass.setUniform("TxPoseMatrix", poseSlice);
        renderPass.setUniform("TxPoseInfo", poseSlice);
        renderPass.setUniform("TxPoseCoords", poseSlice);
        renderPass.setUniform("TxPoseColor", poseSlice);
        renderPass.setUniform("TxPoseNormal", poseSlice);
        renderPass.setUniform("TxCubeRefs", buffer.slice(0L, this.half));
    }

    public GpuBuffer sortedBuffer() {
        GpuBuffer buffer = this.sortedBuffer;

        if (buffer == null)
            throw new IllegalStateException("Sorted GPU cubes of " + this.label + " are drawn before they were uploaded");

        return buffer;
    }

    public void reset() {
        this.poseCount    = 0;
        this.refCount     = 0;
        this.longestRun   = 0;
        this.current      = null;
        this.sortedBuffer = null;
        this.sortedDraws.clear();
    }

    @Override
    public void close() {
        this.texels.close();
        this.sorted.close();
        MemoryUtil.nmemFree(this.poses);
        MemoryUtil.nmemFree(this.refs);
        this.poses = MemoryUtil.NULL;
        this.refs  = MemoryUtil.NULL;
    }

    private void uploadSorted(GpuDevice device) {
        long indexBytes  = 0L;
        long vertexBytes = 0L;

        for (GpuCubeDraw draw : this.sortedDraws) {
            draw.txoptimizations$finishSegments();

            int raw   = draw.txoptimizations$rawVertices();
            int quads = draw.txoptimizations$sortedCubes() * FACES + raw / 4;

            if (raw % 4 != 0)
                throw new IllegalStateException("A sorted draw with GPU cubes has " + raw + " vertices, not whole quads");

            indexBytes += (long) quads * QUAD_INDICES * Integer.BYTES;

            if (raw > 0)
                vertexBytes += ((long) draw.txoptimizations$sortedCubes() * GpuCubes.CUBE_VERTICES + raw) * STRIDE;
        }

        long vertexStart = (indexBytes + ALIGNMENT - 1L) / ALIGNMENT * ALIGNMENT;

        this.sortedBuffer = this.sorted.acquire(device, vertexStart + vertexBytes);

        try (GpuBufferSlice.MappedView view = this.sortedBuffer.slice(0L, vertexStart + vertexBytes).map(false, true)) {
            long base       = MemoryUtil.memAddress(view.data());
            int  firstIndex = 0;
            long vertexAt   = vertexStart;

            for (GpuCubeDraw draw : this.sortedDraws) {
                int raw   = draw.txoptimizations$rawVertices();
                int cubes = draw.txoptimizations$sortedCubes();
                int quads = cubes * FACES + raw / 4;

                if (raw == 0) {
                    writeCubeIndices(base + (long) firstIndex * Integer.BYTES, QuadSort.sort(draw.txoptimizations$sortedKeys(), quads), draw.txoptimizations$sortedRefs());
                    draw.txoptimizations$setSortedDraw(false, firstIndex, quads * QUAD_INDICES, 0L, 0L);
                } else {
                    long bytes = ((long) cubes * GpuCubes.CUBE_VERTICES + raw) * STRIDE;

                    this.expand(draw, base + vertexAt, base + (long) firstIndex * Integer.BYTES, quads);
                    draw.txoptimizations$setSortedDraw(true, firstIndex, quads * QUAD_INDICES, vertexAt, bytes);
                    vertexAt += bytes;
                }

                firstIndex += quads * QUAD_INDICES;
            }
        }
    }

    private void expand(GpuCubeDraw draw, long target, long indices, int quads) {
        long vertices = MemoryUtil.nmemAlloc((long) quads * 4 * STRIDE);

        if (vertices == MemoryUtil.NULL)
            throw new OutOfMemoryError("Could not expand a sorted draw of " + quads + " quads");

        try {
            long at = vertices;

            for (int segment = 0; segment < draw.txoptimizations$segments(); segment ++) {
                int start  = draw.txoptimizations$segmentStart(segment);
                int length = draw.txoptimizations$segmentLength(segment);

                if (!draw.txoptimizations$isCubeSegment(segment)) {
                    copyRaw((StagedVertexBuffer.Draw) draw, start, length, at);
                    at += (long) length * STRIDE;
                    continue;
                }

                for (int ref = start; ref < start + length; ref ++) {
                    this.putCube(at, ref);
                    at += (long) GpuCubes.CUBE_VERTICES * STRIDE;
                }
            }

            if (at != vertices + (long) quads * 4 * STRIDE)
                throw new IllegalStateException("A sorted draw expanded to " + (at - vertices) / STRIDE + " vertices instead of " + quads * 4);

            int[] keys = QuadSort.keys(quads);

            DistanceSorting.fillOriginKeys(vertices, STRIDE, quads, keys, 0);
            TwinQuads.writeIndices(indices, QuadSort.sort(keys, quads), false);
            MemoryUtil.memCopy(vertices, target, (long) quads * 4 * STRIDE);
        } finally {
            MemoryUtil.nmemFree(vertices);
        }
    }

    private void putCube(long target, int ref) {
        long refAt  = this.refs + (long) ref * GpuCubes.REF_BYTES;
        int  shape  = MemoryUtil.memGetInt(refAt);
        long poseAt = this.poses + (long) MemoryUtil.memGetInt(refAt + 4L) * GpuCubes.POSE_BYTES;

        MATRIX.setFromAddress(poseAt).assume(MemoryUtil.memGetInt(poseAt + 64L));

        for (int direction = 0; direction < FACES; direction ++)
            NORMALS[direction] = MemoryUtil.memGetInt(poseAt + 80L + direction * 4L);

        CubeVertices.putShape(target, MATRIX, GpuCubes.shapes(), shape * GpuCubes.shapeFloats(), MemoryUtil.memGetInt(poseAt + 76L), MemoryUtil.memGetLong(poseAt + 68L), NORMALS);
    }

    private static void writeCubeIndices(long pointer, int[] order, int[] refs) {
        for (int quad : order) {
            int first = refs[quad / FACES] * GpuCubes.CUBE_VERTICES + quad % FACES * 4;

            MemoryUtil.memPutInt(pointer, first);
            MemoryUtil.memPutInt(pointer + 4L, first + 1);
            MemoryUtil.memPutInt(pointer + 8L, first + 2);
            MemoryUtil.memPutInt(pointer + 12L, first + 2);
            MemoryUtil.memPutInt(pointer + 16L, first + 3);
            MemoryUtil.memPutInt(pointer + 20L, first);
            pointer += QUAD_INDICES * Integer.BYTES;
        }
    }

    private static void copyRaw(StagedVertexBuffer.Draw draw, int start, int length, long target) {
        long skip      = (long) start * STRIDE;
        long remaining = (long) length * STRIDE;

        for (ByteBufferBuilder.Result slice : ((StagedVertexBufferDrawAccessor) draw).txoptimizations$vertexBufferSlices()) {
            long size = slice.size();

            if (skip >= size) {
                skip -= size;
                continue;
            }

            ByteBufferResultAccessor  result  = (ByteBufferResultAccessor) slice;
            ByteBufferBuilderAccessor builder = (ByteBufferBuilderAccessor) result.txoptimizations$builder();

            if (!builder.txoptimizations$isValid(result.txoptimizations$generation()))
                throw new IllegalStateException("The vertices of a sorted draw were freed before it was expanded");

            long count = Math.min(size - skip, remaining);

            MemoryUtil.memCopy(builder.txoptimizations$pointer() + result.txoptimizations$offset() + skip, target, count);
            target    += count;
            remaining -= count;
            skip       = 0L;

            if (remaining == 0L)
                return;
        }

        throw new IllegalStateException("A sorted draw has " + remaining + " fewer vertex bytes than its segments");
    }

    private static long reallocate(long address, long bytes) {
        long moved = MemoryUtil.nmemRealloc(address, bytes);

        if (moved == MemoryUtil.NULL)
            throw new OutOfMemoryError("Could not grow GPU cube records to " + bytes + " bytes");

        return moved;
    }
}
