package com.teslicek.txoptimizations;

public final class InFlightVertexCopy {

    private static BackgroundVertexCopy owner;

    private InFlightVertexCopy() {
    }

    public static void set(BackgroundVertexCopy copy) {
        if (owner != null && owner != copy)
            throw new IllegalStateException("Another vertex copy is still in flight");

        owner = copy;
    }

    public static void clear(BackgroundVertexCopy copy) {
        if (owner == copy)
            owner = null;
    }

    public static void finish() {
        if (owner != null)
            owner.txoptimizations$finishVertexCopy();
    }
}
