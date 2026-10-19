package com.teslicek.txoptimizations;

public final class ClientClock {

    private static long tick;
    private static long frame;
    private static long frameMillis;

    private ClientClock() {
    }

    public static long tick() {
        return tick;
    }

    public static long frame() {
        return frame;
    }

    public static long frameMillis() {
        return frameMillis;
    }

    public static void advanceTick() {
        tick ++;
    }

    public static void advanceFrame(long millis) {
        frame ++;
        frameMillis = millis;
    }
}
