package com.teslicek.txoptimizations.gpu;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;

final class CpuSampler {

    private static final long         INTERVAL_NANOS = 2_000_000L;
    private static final int          TOP_METHODS    = 45;
    private static final int          TOP_CALLERS    = 3;
    private static final int          TOP_THREADS    = 15;
    private static final ThreadMXBean THREADS        = ManagementFactory.getThreadMXBean();

    private static Thread sampler;
    private static Result result;

    private CpuSampler() {
    }

    static void start(Thread target, long durationNanos) {
        if (sampler != null)
            throw new IllegalStateException("CPU sampler is already running");

        Map<Long, Long> cpuBefore = threadCpuTimes();
        Result          sampled   = new Result(cpuBefore);

        result  = sampled;
        sampler = Thread.ofPlatform().name("TxOptimizations CPU Sampler").daemon().start(() -> sample(target, durationNanos, sampled));
    }

    static String finish() {
        Thread running = sampler;

        if (running == null)
            throw new IllegalStateException("CPU sampler was never started");

        try {
            running.join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the CPU sampler", exception);
        }

        Result finished = result;

        sampler = null;
        result  = null;

        return finished.report();
    }

    private static void sample(Thread target, long durationNanos, Result sampled) {
        long start = System.nanoTime();
        long next  = start;

        while (System.nanoTime() - start < durationNanos) {
            sampled.add(target.getStackTrace());
            next += INTERVAL_NANOS;

            long wait = next - System.nanoTime();

            if (wait > 0L)
                LockSupport.parkNanos(wait);
        }

        sampled.finish(threadCpuTimes(), (System.nanoTime() - start) / 1.0e9);
    }

    private static Map<Long, Long> threadCpuTimes() {
        Map<Long, Long> times = new HashMap<>();

        for (long id : THREADS.getAllThreadIds())
            times.put(id, THREADS.getThreadCpuTime(id));

        return times;
    }

    private static String frameName(StackTraceElement frame) {
        String className = frame.getClassName();
        int    lambda    = className.indexOf("$$Lambda");
        String outer     = lambda < 0 ? className : className.substring(0, lambda);

        return outer.substring(outer.lastIndexOf('.') + 1) + "." + frame.getMethodName();
    }

    private static boolean isPlumbing(StackTraceElement frame) {
        String className = frame.getClassName();

        return className.startsWith("java.") || className.startsWith("jdk.") || className.contains("LambdaForm$") || className.contains("$$Lambda") || frame.getMethodName().startsWith("lambda$");
    }

    private static final class Result {

        private final Map<Long, Long>                   cpuBefore;
        private final Map<String, Integer>              self      = new HashMap<>();
        private final Map<String, Map<String, Integer>> callers   = new HashMap<>();
        private final Map<String, Integer>              inclusive = new HashMap<>();
        private final Set<String>                       seen      = new HashSet<>();
        private Map<Long, Long>                         cpuAfter;
        private double                                  seconds;
        private int                                     samples;

        private Result(Map<Long, Long> cpuBefore) {
            this.cpuBefore = cpuBefore;
        }

        private void add(StackTraceElement[] stack) {
            if (stack.length == 0)
                return;

            this.samples ++;

            String leaf = frameName(stack[0]);

            this.self.merge(leaf, 1, Integer::sum);
            this.callers.computeIfAbsent(leaf, ignored -> new HashMap<>()).merge(this.caller(stack), 1, Integer::sum);
            this.seen.clear();

            for (StackTraceElement frame : stack) {
                String name = frameName(frame);

                if (this.seen.add(name))
                    this.inclusive.merge(name, 1, Integer::sum);
            }
        }

        private String caller(StackTraceElement[] stack) {
            for (int index = 1; index < stack.length; index ++) {
                if (!isPlumbing(stack[index]))
                    return frameName(stack[index]);
            }

            return "<thread root>";
        }

        private void finish(Map<Long, Long> after, double sampledSeconds) {
            this.cpuAfter = after;
            this.seconds  = sampledSeconds;
        }

        private String report() {
            StringBuilder report = new StringBuilder();

            report.append(String.format(Locale.ROOT, "%nCPU: %d render thread samples over %.1f s%n%nRender thread self time%n", this.samples, this.seconds));
            this.self.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(TOP_METHODS)
                .forEach(entry -> {
                    report.append(String.format(Locale.ROOT, "%6.2f%%  %s%n", this.share(entry.getValue()), entry.getKey()));
                    this.callers.get(entry.getKey()).entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(TOP_CALLERS)
                        .forEach(caller -> report.append(String.format(Locale.ROOT, "          %6.2f%%  <- %s%n", this.share(caller.getValue()), caller.getKey())));
                });

            report.append(String.format(Locale.ROOT, "%nRender thread total time including callees%n"));
            this.inclusive.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(TOP_METHODS)
                .forEach(entry -> report.append(String.format(Locale.ROOT, "%6.2f%%  %s%n", this.share(entry.getValue()), entry.getKey())));

            report.append(String.format(Locale.ROOT, "%nCPU time per thread (one core = 100%%)%n"));
            this.cpuAfter.entrySet().stream()
                .filter(entry -> entry.getValue() >= 0L && this.cpuBefore.containsKey(entry.getKey()))
                .map(entry -> Map.entry(entry.getKey(), entry.getValue() - this.cpuBefore.get(entry.getKey())))
                .sorted(Map.Entry.<Long, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(TOP_THREADS)
                .forEach(entry -> report.append(String.format(Locale.ROOT, "%6.1f%%  %s%n", entry.getValue() / 1.0e7 / this.seconds, threadName(entry.getKey()))));

            return report.toString();
        }

        private double share(int count) {
            return count * 100.0 / this.samples;
        }

        private static String threadName(long id) {
            ThreadInfo info = THREADS.getThreadInfo(id);

            if (info == null)
                return "thread " + id + " (ended)";

            return info.getThreadName();
        }
    }
}
