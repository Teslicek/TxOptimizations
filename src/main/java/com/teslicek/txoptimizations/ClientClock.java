package com.teslicek.txoptimizations;

public final class ClientClock {

    private static long          tick;
    private static long          frame;
    private static long          frameMillis;
    private static int           worldChanges;
    private static volatile long renderTick;
    private static volatile long renderFrame;
    private static volatile long renderFrameMillis;
    private static volatile int  renderWorldChanges;

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

    public static int worldChanges() {
        return RenderWorker.isCurrent() ? renderWorldChanges : worldChanges;
    }

    public static void worldChanged() {
        worldChanges ++;
    }

    public static void advanceTick() {
        tick ++;
    }

    public static void advanceFrame(long millis) {
        frame ++;
        frameMillis = millis;
    }

    public static void handOff() {
        renderTick         = tick;
        renderFrame        = frame;
        renderFrameMillis  = frameMillis;
        renderWorldChanges = worldChanges;
    }
}
