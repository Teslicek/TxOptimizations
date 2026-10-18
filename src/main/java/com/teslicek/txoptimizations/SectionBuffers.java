package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import java.util.Arrays;
import org.jspecify.annotations.Nullable;

public final class SectionBuffers {

    private static final int INITIAL_CAPACITY = 8;

    private static @Nullable GpuBuffer[] buffers = new GpuBuffer[INITIAL_CAPACITY];
    private static int[]                 hashes  = new int[INITIAL_CAPACITY];

    private SectionBuffers() {
    }

    public static int register(GpuBuffer buffer) {
        int id = 0;

        while (id < buffers.length && buffers[id] != null)
            id ++;

        if (id == buffers.length) {
            buffers = Arrays.copyOf(buffers, buffers.length * 2);
            hashes  = Arrays.copyOf(hashes, hashes.length * 2);
        }

        buffers[id] = buffer;
        hashes[id]  = buffer.hashCode();

        return id;
    }

    public static void release(GpuBuffer buffer) {
        for (int id = 0; id < buffers.length; id ++) {
            if (buffers[id] == buffer) {
                buffers[id] = null;
                return;
            }
        }

        throw new IllegalStateException(buffer + " was never registered as a section buffer");
    }

    public static GpuBuffer buffer(int id) {
        GpuBuffer buffer = buffers[id];

        if (buffer == null)
            throw new IllegalStateException("Section buffer " + id + " was already released");

        return buffer;
    }

    public static int hash(int id) {
        return hashes[id];
    }
}
