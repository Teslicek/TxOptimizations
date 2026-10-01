package com.teslicek.txoptimizations.hud;

import java.util.Arrays;
import net.minecraft.client.DeltaTracker;

public final class HudDeltaTracker {

    private static final float[] REALTIME  = new float[HudCache.PASSES + 1];
    private static final float[] GAME_TIME = new float[HudCache.PASSES + 1];

    private static float   realtimeDelta;
    private static float   gameTimeDelta;
    private static boolean ready;

    private HudDeltaTracker() {
    }

    public static void accumulate(DeltaTracker.Timer timer, int pass) {
        REALTIME[pass]  += timer.getRealtimeDeltaTicks();
        GAME_TIME[pass] += timer.getGameTimeDeltaTicks();
    }

    public static void disable() {
        ready = false;
    }

    public static float realtimeDelta(float vanilla) {
        return HudCache.isRendering() && ready ? realtimeDelta : vanilla;
    }

    public static float gameTimeDelta(float vanilla) {
        return HudCache.isRendering() && ready ? gameTimeDelta : vanilla;
    }

    static void completeCycle() {
        realtimeDelta = 0.0F;
        gameTimeDelta = 0.0F;

        for (int pass = 0; pass <= HudCache.PASSES; pass ++) {
            realtimeDelta += REALTIME[pass];
            gameTimeDelta += GAME_TIME[pass];
        }

        ready = true;
        Arrays.fill(REALTIME, 0.0F);
        Arrays.fill(GAME_TIME, 0.0F);
    }
}
