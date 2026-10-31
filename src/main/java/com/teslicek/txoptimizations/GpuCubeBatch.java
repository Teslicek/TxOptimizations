package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import java.util.Arrays;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

public final class GpuCubeBatch implements AutoCloseable {

    private static final int  INITIAL_POSES = 256;
    private static final int  INITIAL_REFS  = 1024;
    private static final int  ALIGNMENT     = 256;
    private static final long BUSY_FRAMES   = 3L;

    private final String label;

    private long                     poses = MemoryUtil.nmemAlloc((long) INITIAL_POSES * GpuCubes.POSE_BYTES);
    private int                      poseCapacity = INITIAL_POSES;
    private int                      poseCount;
    private long                     refs = MemoryUtil.nmemAlloc((long) INITIAL_REFS * GpuCubes.REF_BYTES);
    private int                      refCapacity = INITIAL_REFS;
    private int                      refCount;
    private int                      longestRun;
    private GpuBuffer[]              buffers = new GpuBuffer[4];
    private long[]                   usedAt = new long[4];
    private @Nullable GpuBuffer      current;
    private long                     half;

    public GpuCubeBatch(String label) {
        if (this.poses == MemoryUtil.NULL || this.refs == MemoryUtil.NULL)
            throw new OutOfMemoryError("Could not allocate GPU cube records for " + label);

        this.label = label;
        Arrays.fill(this.usedAt, Long.MIN_VALUE / 2);
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

    public void upload(GpuDevice device) {
        if (this.current != null)
            throw new IllegalStateException("GPU cubes of " + this.label + " were uploaded twice before their draws ended");

        if (this.refCount == 0)
            return;

        long poseBytes = (long) this.poseCount * GpuCubes.POSE_BYTES;
        long refBytes  = (long) this.refCount * GpuCubes.REF_BYTES;

        this.current = this.acquire(device, 2L * Math.max(poseBytes, refBytes));
        this.half    = this.current.size() / 2L;

        try (GpuBufferSlice.MappedView view = this.current.slice(0L, this.half + poseBytes).map(false, true)) {
            long target = MemoryUtil.memAddress(view.data());

            MemoryUtil.memCopy(this.refs, target, refBytes);
            MemoryUtil.memCopy(this.poses, target + this.half, poseBytes);
        }

        GpuCubes.uploadStatic(device);
        GpuCubes.requestIndices(this.longestRun);
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

    public void reset() {
        this.poseCount    = 0;
        this.refCount     = 0;
        this.longestRun = 0;
        this.current    = null;
    }

    @Override
    public void close() {
        for (GpuBuffer buffer : this.buffers) {
            if (buffer != null)
                GpuCubes.retire(buffer);
        }

        Arrays.fill(this.buffers, null);
        MemoryUtil.nmemFree(this.poses);
        MemoryUtil.nmemFree(this.refs);
        this.poses = MemoryUtil.NULL;
        this.refs  = MemoryUtil.NULL;
    }

    private GpuBuffer acquire(GpuDevice device, long size) {
        long now  = ClientClock.frame();
        int  free = -1;

        for (int slot = 0; slot < this.buffers.length; slot ++) {
            if (now - this.usedAt[slot] < BUSY_FRAMES)
                continue;

            if (this.buffers[slot] != null && this.buffers[slot].size() >= size) {
                this.usedAt[slot] = now;

                return this.buffers[slot];
            }

            if (free == -1)
                free = slot;
        }

        if (free == -1) {
            free          = this.buffers.length;
            this.buffers  = Arrays.copyOf(this.buffers, free * 2);
            this.usedAt   = Arrays.copyOf(this.usedAt, free * 2);
            Arrays.fill(this.usedAt, free, free * 2, Long.MIN_VALUE / 2);
        }

        if (this.buffers[free] != null)
            GpuCubes.retire(this.buffers[free]);

        long capacity = Long.highestOneBit(Math.max(ALIGNMENT, size - 1)) << 1;

        this.buffers[free] = device.createBuffer(() -> "TxOptimizations GPU cubes of " + this.label, GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER | GpuBuffer.USAGE_MAP_WRITE, capacity);
        this.usedAt[free]  = now;

        return this.buffers[free];
    }

    private static long reallocate(long address, long bytes) {
        long moved = MemoryUtil.nmemRealloc(address, bytes);

        if (moved == MemoryUtil.NULL)
            throw new OutOfMemoryError("Could not grow GPU cube records to " + bytes + " bytes");

        return moved;
    }
}
