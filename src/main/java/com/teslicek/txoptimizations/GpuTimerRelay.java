package com.teslicek.txoptimizations;

import com.mojang.blaze3d.systems.TimerQuery;

public final class GpuTimerRelay {

    private static volatile TimerQuery.Status status = TimerQuery.Status.NOT_RECORDING;
    private static volatile long              nanos;

    private GpuTimerRelay() {
    }

    public static TimerQuery.Status status() {
        return status;
    }

    public static long nanos() {
        return nanos;
    }

    public static void publish(TimerQuery query) {
        if (!RenderWorker.isCurrent())
            throw new IllegalStateException("GPU timer results were published outside the render worker");

        status = query.getStatus();
        nanos  = query.get();
    }
}
