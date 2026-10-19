package com.teslicek.txoptimizations;

public final class FrameSpin {

    public static final long SPIN_NANOS = 250_000L;

    private static final long FAST_FRAME_NANOS = 250_000L;
    private static final int  SMOOTHING_SHIFT  = 4;

    private static volatile boolean fastFrames;
    private static long             averageFrameNanos;

    private FrameSpin() {
    }

    public static void recordFrame(long frameNanos) {
        averageFrameNanos = averageFrameNanos == 0L ? frameNanos : averageFrameNanos + (frameNanos - averageFrameNanos >> SMOOTHING_SHIFT);
        fastFrames        = averageFrameNanos < FAST_FRAME_NANOS;
    }

    public static boolean isFastFrames() {
        return fastFrames;
    }
}
