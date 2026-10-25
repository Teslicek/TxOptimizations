package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;

public final class InFlightVertexCopy {

    private static BackgroundVertexCopy owner;
    private static ByteBufferBuilder    source;

    private InFlightVertexCopy() {
    }

    public static void set(BackgroundVertexCopy copy, ByteBufferBuilder staging) {
        if (owner != null && owner != copy)
            throw new IllegalStateException("Another vertex copy is still in flight");

        owner  = copy;
        source = staging;
    }

    public static void clear(BackgroundVertexCopy copy) {
        if (owner != copy)
            return;

        owner  = null;
        source = null;
    }

    public static void finish() {
        if (owner != null)
            owner.txoptimizations$finishVertexCopy();
    }

    public static void awaitBeforeMoving(ByteBufferBuilder staging) {
        if (staging == source)
            owner.txoptimizations$awaitVertexCopy();
    }
}
