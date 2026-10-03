package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.device.GpuSurface;
import com.mojang.renderpearl.backend.vulkan.VulkanQueue;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public final class PresentThread {

    private static final int                        SPINS    = 1024;
    private static final ConcurrentLinkedQueue<Job> JOBS     = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger              PENDING  = new AtomicInteger();
    private static final AtomicInteger              PRESENTS = new AtomicInteger();
    private static final Thread                     WORKER   = Thread.ofPlatform().name("TxOptimizations Present").daemon().priority(Thread.MAX_PRIORITY).start(PresentThread::work);

    private static volatile boolean   sleeping;
    private static volatile Thread    waiting;
    private static volatile Throwable failure;
    private static GpuSurface         armed;
    private static Runnable           captured;
    private static boolean            capturing;

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

    public static void submit(VulkanQueue.Submission submission) {
        dispatch(new Job(submission::close, false));
    }

    public static void handOff(VulkanQueue.Submission submission) {
        GpuSurface surface = armed;

        armed     = null;
        capturing = true;

        try {
            surface.present();
        } finally {
            capturing = false;
        }

        Runnable present = captured;

        captured = null;

        if (present == null)
            throw new IllegalStateException("The surface did not hand over its present");

        dispatch(new Job(() -> {
            submission.close();
            present.run();
        }, true));
    }

    public static boolean isCapturing() {
        return capturing;
    }

    public static void capture(Runnable present) {
        if (captured != null)
            throw new IllegalStateException("A present was already captured for this frame");

        captured = present;
    }

    public static void drain() {
        await(PENDING);
    }

    public static void drainPresents() {
        await(PRESENTS);
    }

    private static void dispatch(Job job) {
        PENDING.incrementAndGet();

        if (job.present())
            PRESENTS.incrementAndGet();

        JOBS.add(job);

        if (sleeping)
            LockSupport.unpark(WORKER);
    }

    private static void await(AtomicInteger counter) {
        if (Thread.currentThread() == WORKER)
            return;

        if (counter.get() != 0) {
            waiting = Thread.currentThread();

            int spins = 0;

            while (counter.get() != 0) {
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
            Job job = JOBS.poll();

            if (job == null) {
                sleeping = true;

                if (JOBS.isEmpty())
                    LockSupport.park(PresentThread.class);

                sleeping = false;
                continue;
            }

            try {
                job.task().run();
            } catch (Throwable thrown) {
                if (failure == null)
                    failure = thrown;
            }

            if (job.present())
                PRESENTS.decrementAndGet();

            PENDING.decrementAndGet();
            LockSupport.unpark(waiting);
        }
    }

    private record Job(Runnable task, boolean present) {
    }
}
