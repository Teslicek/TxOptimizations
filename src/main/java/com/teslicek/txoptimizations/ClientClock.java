package com.teslicek.txoptimizations;

public final class ClientClock {

    private static long          tick;
    private static long          frame;
    private static long          frameMillis;
    private static volatile long renderTick;
    private static volatile long renderFrame;
    private static volatile long renderFrameMillis;

    private ClientClock() {
    }

    public static long tick() {
        return RenderWorker.isCurrent() ? renderTick : tick;
    }

    public static long frame() {
        return RenderWorker.isCurrent() ? renderFrame : frame;
    }

    public static long frameMillis() {
        return RenderWorker.isCurrent() ? renderFrameMillis : frameMillis;
    }

    public static void advanceTick() {
        tick ++;
    }

    public static void advanceFrame(long millis) {
        frame ++;
        frameMillis = millis;
    }

    public static void handOff() {
        renderTick        = tick;
        renderFrame       = frame;
        renderFrameMillis = frameMillis;
    }
}
