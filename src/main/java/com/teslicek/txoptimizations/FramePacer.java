package com.teslicek.txoptimizations;

public final class FramePacer {

    private static final long STALL_NANOS     = 300_000L;
    private static final long LONGEST_NANOS   = 4_000_000L;
    private static final int  RAISE_SHIFT     = 6;
    private static final int  LOWER_SHIFT     = 11;
    private static final int  SMOOTHING_SHIFT = 4;

    private static long lastStart;
    private static long averageInterval;
    private static long target;
    private static long paced;

    private FramePacer() {
    }

    public static void awaitStart() {
        long now = System.nanoTime();

        if (target > 0L && lastStart != 0L) {
            long due = lastStart + target;

            if (now < due) {
                long waitStart = now;

                while (now < due) {
                    Thread.onSpinWait();
                    now = System.nanoTime();
                }

                paced += now - waitStart;
            }
        }

        if (lastStart != 0L) {
            long interval = now - lastStart;

            averageInterval = averageInterval == 0L ? interval : averageInterval + (interval - averageInterval >> SMOOTHING_SHIFT);
        }

        lastStart = now;

        if (target > 0L)
            target -= target >> LOWER_SHIFT;
    }

    public static void recordAcquire(long nanos) {
        if (nanos < STALL_NANOS)
            return;

        long base = Math.max(target, averageInterval);

        target = Math.min(LONGEST_NANOS, base + Math.max(1L, base >> RAISE_SHIFT));
    }

    public static long takePacedNanos() {
        long nanos = paced;

        paced = 0L;

        return nanos;
    }
}
