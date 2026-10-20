package com.teslicek.txoptimizations;

import java.util.concurrent.atomic.AtomicLong;

public final class TerrainDrawChanges {

    private static final AtomicLong CHANGES = new AtomicLong();

    private TerrainDrawChanges() {
    }

    public static void mark() {
        CHANGES.incrementAndGet();
    }

    public static long count() {
        return CHANGES.get();
    }
}
