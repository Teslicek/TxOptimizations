package com.teslicek.txoptimizations;

import com.teslicek.txoptimizations.gpu.GpuPassProfiler;

public final class FramePath {

    private static final long   PROBE_INTERVAL_NANOS = 5_000_000_000L;
    private static final int    PROBE_FRAMES         = 64;
    private static final int    SETTLE_FRAMES        = 4;
    private static final double STEADY_WEIGHT        = 1.0 / 64.0;
    private static final double REQUIRED_GAIN        = 0.98;

    private static boolean worker = true;
    private static boolean probing;
    private static boolean frameUsesWorker;
    private static long    lastSubmit;
    private static long    nextProbe;
    private static double  steadyNanos;
    private static long    probeNanos;
    private static int     probeFrames;
    private static long    workerFrames;
    private static long    inlineFrames;

    private FramePath() {
    }

    public static boolean beginFrame(boolean eligible) {
        frameUsesWorker = eligible && (worker != probing);

        return frameUsesWorker;
    }

    public static boolean frameUsesWorker() {
        return frameUsesWorker;
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
            nextProbe  = now + PROBE_INTERVAL_NANOS;

            return;
        }

        long frameNanos = now - lastSubmit;

        lastSubmit = now;
        GpuPassProfiler.recordFrameTime(frameNanos);

        if (!probing) {
            steadyNanos = steadyNanos == 0.0 ? frameNanos : steadyNanos + (frameNanos - steadyNanos) * STEADY_WEIGHT;

            if (now >= nextProbe) {
                probing     = true;
                probeNanos  = 0L;
                probeFrames = 0;
            }

            return;
        }

        probeFrames ++;

        if (probeFrames <= SETTLE_FRAMES)
            return;

        probeNanos += frameNanos;

        if (probeFrames < SETTLE_FRAMES + PROBE_FRAMES)
            return;

        double probeAverage = (double) probeNanos / PROBE_FRAMES;

        if (probeAverage < steadyNanos * REQUIRED_GAIN) {
            worker      = !worker;
            steadyNanos = probeAverage;
        }

        probing   = false;
        nextProbe = now + PROBE_INTERVAL_NANOS;
    }
}
