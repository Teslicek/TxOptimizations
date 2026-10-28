package com.teslicek.txoptimizations;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

public final class ParticleLightShare {

    private static final int                 MISSING = -1;
    private static final Long2IntOpenHashMap LIGHT   = lightMap();

    private static boolean active;

    private ParticleLightShare() {
    }

    public static void begin() {
        if (active)
            throw new IllegalStateException("Particle light sharing is already active");

        LIGHT.clear();
        active = true;
    }

    public static void end() {
        active = false;
    }

    public static boolean isActive() {
        return active;
    }

    public static int find(long block) {
        return LIGHT.get(block);
    }

    public static boolean isMissing(int light) {
        return light == MISSING;
    }

    public static void store(long block, int light) {
        LIGHT.put(block, light);
    }

    private static Long2IntOpenHashMap lightMap() {
        Long2IntOpenHashMap light = new Long2IntOpenHashMap();

        light.defaultReturnValue(MISSING);

        return light;
    }
}
