package com.teslicek.txoptimizations;

import com.mojang.logging.LogUtils;
import java.util.concurrent.locks.LockSupport;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;

public final class RenderWorker {

    private static final Logger LOGGER     = LogUtils.getLogger();
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

    public static void start(Runnable frame) {
        awaitFrame();
        RenderThreadStack.ensureCaptured();
        workerStack();

        job = frame;

        if (parked)
            LockSupport.unpark(WORKER);
    }

    public static MemoryStack stack() {
        return stack;
    }

    private static MemoryStack workerStack() {
        while (stack == null)
            Thread.onSpinWait();

        return stack;
    }

    public static void awaitFrame() {
        awaitIdle();
        rethrowFailure();
    }

    public static void awaitFromMain() {
        if (RenderThreadStack.isMain())
            awaitIdle();
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

    private static void rethrowFailure() {
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
                LOGGER.error("The render job failed", thrown);
                failure = thrown;
            }

            job = null;

            Thread blocked = waiting;

            if (blocked != null)
                LockSupport.unpark(blocked);
        }
    }
}
