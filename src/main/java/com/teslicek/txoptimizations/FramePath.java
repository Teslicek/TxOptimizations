package com.teslicek.txoptimizations;

import com.teslicek.txoptimizations.gpu.GpuPassProfiler;

public final class FramePath {

    private static final int    WINDOW_FRAMES = 128;
    private static final double WAIT_LIMIT    = 0.10;
    private static final double ACQUIRE_LIMIT = 0.08;

    private static boolean worker;
    private static boolean frameUsesWorker;
    private static boolean frameEligible;
    private static long    lastSubmit;
    private static long    windowFrameNanos;
    private static long    windowWaitNanos;
    private static long    windowAcquireNanos;
    private static int     windowFrames;
    private static long    workerFrames;
    private static long    inlineFrames;

    private FramePath() {
    }

    public static boolean beginFrame(boolean eligible) {
        frameEligible   = eligible;
        frameUsesWorker = eligible && worker;

        return frameUsesWorker;
    }

    public static boolean frameUsesWorker() {
        return frameUsesWorker;
    }

    public static void recordGpuWait(long nanos) {
        windowWaitNanos += nanos;
    }

    public static void recordAcquire(long nanos) {
        windowAcquireNanos += nanos;
    }

    public static void resetCounts() {
        workerFrames = 0L;
        inlineFrames = 0L;
    }

    public static double workerShare() {
        long frames = workerFrames + inlineFrames;

        if (frames == 0L)
            throw new IllegalStateException("No frames were submitted while counting");

        return (double) workerFrames / frames;
    }

    public static void frameSubmitted() {
        long now = System.nanoTime();

        if (frameUsesWorker)
            workerFrames ++;
        else
            inlineFrames ++;

        if (lastSubmit == 0L) {
            lastSubmit = now;

            return;
        }

        long frameNanos = now - lastSubmit;

        lastSubmit = now;
        GpuPassProfiler.recordFrameTime(frameNanos);

        if (!frameEligible) {
            resetWindow();

            return;
        }

        windowFrameNanos += frameNanos;
        windowFrames ++;

        if (windowFrames < WINDOW_FRAMES)
            return;

        if (worker)
            worker = windowAcquireNanos <= windowFrameNanos * ACQUIRE_LIMIT;
        else
            worker = windowWaitNanos < windowFrameNanos * WAIT_LIMIT;

        resetWindow();
    }

    private static void resetWindow() {
        windowFrameNanos   = 0L;
        windowWaitNanos    = 0L;
        windowAcquireNanos = 0L;
        windowFrames       = 0;
    }
}
