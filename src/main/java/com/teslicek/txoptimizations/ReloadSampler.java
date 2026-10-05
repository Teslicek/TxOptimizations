package com.teslicek.txoptimizations;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import org.slf4j.Logger;

public final class ReloadSampler {

    private static final Logger LOGGER         = LogUtils.getLogger();
    private static final long   INTERVAL_NANOS = 1_000_000L;
    private static final int    TOP            = 25;

    private static volatile boolean running;
    private static Thread           sampler;
    private static Map<String, Integer> leaves;
    private static Map<String, Integer> owners;
    private static int                  samples;

    private ReloadSampler() {
    }

    public static void start() {
        if (sampler != null)
            throw new IllegalStateException("A reload is already being sampled");

        List<Thread> workers = Thread.getAllStackTraces().keySet().stream().filter(thread -> thread.getName().startsWith("Worker-Main")).toList();

        if (workers.isEmpty())
            throw new IllegalStateException("No Worker-Main threads to sample");

        leaves  = new HashMap<>();
        owners  = new HashMap<>();
        samples = 0;
        running = true;
        sampler = new Thread(() -> sample(workers), "TxOptimizations reload sampler");
        sampler.setDaemon(true);
        sampler.start();
    }

    public static void finish() {
        Thread thread = sampler;

        if (thread == null)
            throw new IllegalStateException("A profiled reload finished without its worker sampler");

        running = false;

        try {
            thread.join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException("Interrupted while stopping the reload sampler", exception);
        }

        sampler = null;
        LOGGER.info("Worker threads: {} busy samples (1 sample = 1 ms of one thread)", samples);
        log("Busiest methods", leaves);
        log("Busiest game code (first game frame on the stack)", owners);
    }

    private static void sample(List<Thread> workers) {
        long next = System.nanoTime();

        while (running) {
            for (Thread worker : workers) {
                if (worker.getState() != Thread.State.RUNNABLE)
                    continue;

                StackTraceElement[] stack = worker.getStackTrace();

                if (stack.length == 0)
                    continue;

                samples ++;
                leaves.merge(frame(stack[0]), 1, Integer::sum);

                for (StackTraceElement element : stack) {
                    if (isGameCode(element.getClassName())) {
                        owners.merge(frame(element), 1, Integer::sum);

                        break;
                    }
                }
            }

            next += INTERVAL_NANOS;
            LockSupport.parkNanos(next - System.nanoTime());
        }
    }

    private static boolean isGameCode(String className) {
        return className.startsWith("net.minecraft.") || className.startsWith("com.mojang.") || className.startsWith("com.teslicek.") || className.startsWith("net.caffeinemc.");
    }

    private static String frame(StackTraceElement element) {
        String className = element.getClassName();

        return className.substring(className.lastIndexOf('.') + 1) + "." + element.getMethodName();
    }

    private static void log(String title, Map<String, Integer> counts) {
        LOGGER.info("{}:", title);
        counts.entrySet().stream()
            .sorted((first, second) -> Integer.compare(second.getValue(), first.getValue()))
            .limit(TOP)
            .forEach(entry -> LOGGER.info("  {}  {}", entry.getValue(), entry.getKey()));
    }
}
