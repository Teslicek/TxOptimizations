package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.device.GpuSurface;
import com.mojang.renderpearl.backend.vulkan.VulkanQueue;
import java.util.Optional;
import java.util.concurrent.locks.LockSupport;

public final class PresentThread {

    private static final int    SPINS  = 1024;
    private static final Thread WORKER = Thread.ofPlatform().name("TxOptimizations Present").daemon().priority(Thread.MAX_PRIORITY).start(PresentThread::work);

    private static volatile Runnable  job;
    private static volatile Thread    waiting;
    private static volatile Throwable failure;
    private static GpuSurface         armed;
    private static Runnable           captured;
    private static boolean            capturing;
    private static boolean            acquireAhead;

    private PresentThread() {
    }

    public static void arm(GpuSurface frameSurface) {
        if (armed != null)
            throw new IllegalStateException("The frame present is already armed");

        armed = frameSurface;
    }

    public static void disarm() {
        armed = null;
    }

    public static boolean isArmed() {
        return armed != null;
    }

    public static boolean canDeferAcquire(GpuSurface surface) {
        Optional<GpuSurface.Configuration> config = surface.currentConfiguration();

        return config.isPresent() && config.get().presentMode() == GpuSurface.PresentMode.IMMEDIATE;
    }

    public static void handOff(VulkanQueue.Submission submission) {
        GpuSurface surface = armed;

        armed        = null;
        capturing    = true;
        acquireAhead = canDeferAcquire(surface);

        try {
            surface.present();
        } finally {
            capturing = false;
        }

        Runnable present = captured;

        captured = null;

        if (present == null)
            throw new IllegalStateException("The surface did not hand over its present");

        drain();
        job = () -> {
            submission.close();
            present.run();
        };
        LockSupport.unpark(WORKER);
    }

    public static boolean isCapturing() {
        return capturing;
    }

    public static boolean acquiresAhead() {
        return acquireAhead;
    }

    public static boolean isWorker() {
        return Thread.currentThread() == WORKER;
    }

    public static void capture(Runnable present) {
        if (captured != null)
            throw new IllegalStateException("A present was already captured for this frame");

        captured = present;
    }

    public static void drain() {
        if (Thread.currentThread() == WORKER)
            return;

        if (job != null) {
            waiting = Thread.currentThread();

            int spins = 0;

            while (job != null) {
                if (spins < SPINS) {
                    spins ++;
                    Thread.onSpinWait();
                } else {
                    LockSupport.park(PresentThread.class);
                }
            }

            waiting = null;
        }

        Throwable thrown = failure;

        if (thrown == null)
            return;

        failure = null;

        if (thrown instanceof RuntimeException runtime)
            throw runtime;

        if (thrown instanceof Error error)
            throw error;

        throw new IllegalStateException("The present thread failed", thrown);
    }

    private static void work() {
        while (true) {
            Runnable next = job;

            if (next == null) {
                LockSupport.park(PresentThread.class);
                continue;
            }

            try {
                next.run();
            } catch (Throwable thrown) {
                failure = thrown;
            }

            job = null;
            LockSupport.unpark(waiting);
        }
    }
}
