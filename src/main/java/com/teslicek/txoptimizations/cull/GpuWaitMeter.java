package com.teslicek.txoptimizations.cull;

public final class GpuWaitMeter {

    private static final long   WINDOW     = 500_000_000L;
    private static final double ENABLE_AT  = 0.10;
    private static final double DISABLE_AT = 0.03;

    private static long    windowStart = System.nanoTime();
    private static long    waited;
    private static long    waitStart;
    private static boolean gpuBound;

    private GpuWaitMeter() {
    }

    public static void beginWait() {
        waitStart = System.nanoTime();
    }

    public static void endWait() {
        long now = System.nanoTime();

        waited += now - waitStart;

        if (now - windowStart < WINDOW)
            return;

        double share = (double) waited / (now - windowStart);

        if (share > ENABLE_AT)
            gpuBound = true;
        else if (share < DISABLE_AT)
            gpuBound = false;

        windowStart = now;
        waited      = 0L;
    }

    public static boolean isGpuBound() {
        return gpuBound;
    }
}
