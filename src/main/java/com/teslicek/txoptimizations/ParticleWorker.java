package com.teslicek.txoptimizations;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public final class ParticleWorker {

    private static final AtomicReference<Runnable> JOB    = new AtomicReference<>();
    private static final Thread                    THREAD = Thread.ofPlatform().name("TxOptimizations Particle Worker").daemon().priority(Thread.MAX_PRIORITY).start(ParticleWorker::work);

    private static volatile boolean   done = true;
    private static volatile Throwable failure;

    private ParticleWorker() {
    }

    public static void start(Runnable job) {
        if (!done)
            throw new IllegalStateException("A particle extraction is already running");

        failure = null;
        done    = false;
        JOB.set(job);
        LockSupport.unpark(THREAD);
    }

    public static void join() {
        Runnable pending = JOB.getAndSet(null);

        if (pending != null)
            run(pending);

        while (!done)
            Thread.onSpinWait();

        Throwable thrown = failure;

        if (thrown == null)
            return;

        failure = null;

        if (thrown instanceof RuntimeException runtime)
            throw runtime;

        throw new IllegalStateException("Extracting particles on " + THREAD.getName() + " failed", thrown);
    }

    private static void work() {
        while (true) {
            Runnable next = JOB.getAndSet(null);

            if (next == null) {
                LockSupport.park();
                continue;
            }

            run(next);
        }
    }

    private static void run(Runnable next) {
        try {
            next.run();
        } catch (Throwable thrown) {
            failure = thrown;
        } finally {
            done = true;
        }
    }
}
