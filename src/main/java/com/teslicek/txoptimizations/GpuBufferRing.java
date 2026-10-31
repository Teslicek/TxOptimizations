package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.device.GpuDevice;
import java.util.Arrays;

public final class GpuBufferRing implements AutoCloseable {

    private static final int  MINIMUM     = 512;
    private static final long BUSY_FRAMES = 3L;

    private final String label;
    private final int    usage;

    private GpuBuffer[] buffers = new GpuBuffer[4];
    private long[]      usedAt  = new long[4];

    public GpuBufferRing(String label, int usage) {
        this.label = label;
        this.usage = usage;
        Arrays.fill(this.usedAt, Long.MIN_VALUE / 2);
    }

    public GpuBuffer acquire(GpuDevice device, long size) {
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
            free         = this.buffers.length;
            this.buffers = Arrays.copyOf(this.buffers, free * 2);
            this.usedAt  = Arrays.copyOf(this.usedAt, free * 2);
            Arrays.fill(this.usedAt, free, free * 2, Long.MIN_VALUE / 2);
        }

        if (this.buffers[free] != null)
            GpuCubes.retire(this.buffers[free]);

        long capacity = Long.highestOneBit(Math.max(MINIMUM, size - 1)) << 1;

        this.buffers[free] = device.createBuffer(() -> this.label, this.usage, capacity);
        this.usedAt[free]  = now;

        return this.buffers[free];
    }

    @Override
    public void close() {
        for (GpuBuffer buffer : this.buffers) {
            if (buffer != null)
                GpuCubes.retire(buffer);
        }

        Arrays.fill(this.buffers, null);
    }
}
