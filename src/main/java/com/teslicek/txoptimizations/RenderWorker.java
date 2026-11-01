package com.teslicek.txoptimizations;

import java.util.concurrent.locks.LockSupport;
import org.lwjgl.system.MemoryStack;

public final class RenderWorker {

    private static final long   SPIN_NANOS = 200_000L;
    private static final Thread WORKER     = Thread.ofPlatform().name("TxOptimizations Render").daemon().priority(Thread.MAX_PRIORITY).start(RenderWorker::work);

    private static volatile Runnable    job;
    private static volatile Throwable   failure;
    private static volatile Thread      waiting;
    private static volatile boolean     parked;
    private static volatile MemoryStack stack;

    private RenderWorker() {
    }

    public static boolean isCurrent() {
        return Thread.currentThread() == WORKER;
    }

    public static void runAndWait(Runnable frame) {
        if (job != null)
            throw new IllegalStateException("A render frame is already running on the render worker");

        RenderThreadStack.ensureCaptured();

        if (!RenderThreadStack.isOwner())
            throw new IllegalStateException("A render frame was handed off from " + Thread.currentThread().getName() + ", which does not own rendering");

        Thread      owner      = Thread.currentThread();
        MemoryStack ownerStack = RenderThreadStack.stack();

        RenderThreadStack.handTo(WORKER, workerStack());
        job = frame;

        if (parked)
            LockSupport.unpark(WORKER);

        awaitIdle();
        RenderThreadStack.handTo(owner, ownerStack);

        Throwable thrown = failure;

        if (thrown == null)
            return;

        failure = null;

        if (thrown instanceof RuntimeException runtime)
            throw runtime;

        if (thrown instanceof Error error)
            throw error;

        throw new IllegalStateException("The render worker failed", thrown);
    }

    private static MemoryStack workerStack() {
        while (stack == null)
            Thread.onSpinWait();

        return stack;
    }

    private static void awaitIdle() {
        long deadline = System.nanoTime() + SPIN_NANOS;

        while (job != null) {
            if (System.nanoTime() < deadline) {
                Thread.onSpinWait();
                continue;
            }

            waiting = Thread.currentThread();

            if (job != null)
                LockSupport.park(RenderWorker.class);

            waiting = null;
        }
    }

    private static void work() {
        stack = MemoryStack.stackGet();

        while (true) {
            Runnable next = job;

            if (next == null) {
                long deadline = System.nanoTime() + SPIN_NANOS;

                while (job == null && System.nanoTime() < deadline)
                    Thread.onSpinWait();

                parked = true;

                if (job == null)
                    LockSupport.park(RenderWorker.class);

                parked = false;
                continue;
            }

            try {
                next.run();
            } catch (Throwable thrown) {
                failure = thrown;
            }

            job = null;

            Thread blocked = waiting;

            if (blocked != null)
                LockSupport.unpark(blocked);
        }
    }
}
