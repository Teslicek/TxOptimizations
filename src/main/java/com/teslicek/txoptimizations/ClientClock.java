package com.teslicek.txoptimizations;

public final class ClientClock {

    private static long tick;
    private static long frame;

    private ClientClock() {
    }

    public static long tick() {
        return tick;
    }

    public static long frame() {
        return frame;
    }

    public static void advanceTick() {
        tick ++;
    }

    public static void advanceFrame() {
        frame ++;
    }
}
