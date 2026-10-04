package com.teslicek.txoptimizations.gpu;

import java.lang.management.ManagementFactory;
import com.sun.management.ThreadMXBean;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;

final class CpuSampler {

    private static final long         INTERVAL_NANOS    = 2_000_000L;
    private static final int          TOP_CALLERS       = 3;
    private static final String       NATIVE_TRAMPOLINE = "org.lwjgl.system.JNI";
    private static final ThreadMXBean THREADS           = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static final double       SLOW_FRACTION     = 0.99;
    private static final int          WORST_FRAMES      = 10;

    private static Thread sampler;
    private static Result result;

    private CpuSampler() {
    }

    static void start(Thread target, long durationNanos) {
        if (sampler != null)
            throw new IllegalStateException("CPU sampler is already running");

        Map<Long, Long> cpuBefore   = threadCpuTimes();
        Map<Long, Long> allocBefore = threadAllocations();
        Result          sampled     = new Result(target.threadId(), cpuBefore, allocBefore);

        result  = sampled;
        sampler = Thread.ofPlatform().name("TxOptimizations CPU Sampler").daemon().start(() -> sample(target, durationNanos, sampled));
    }

    static String finish(long[] frameEnds, long[] frameTimes, int frameCount, List<GcPauses.Pause> pauses) {
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

        return finished.report() + finished.slowFrameReport(frameEnds, frameTimes, frameCount, pauses);
    }

    private static void sample(Thread target, long durationNanos, Result sampled) {
        long start = System.nanoTime();
        long next  = start;

        while (System.nanoTime() - start < durationNanos) {
            long sampledAt = System.nanoTime();

            sampled.add(sampledAt, target.getStackTrace());
            next += INTERVAL_NANOS;

            long wait = next - System.nanoTime();

            if (wait > 0L)
                LockSupport.parkNanos(wait);
        }

        sampled.finish(threadCpuTimes(), threadAllocations(), (System.nanoTime() - start) / 1.0e9);
    }

    private static Map<Long, Long> threadAllocations() {
        Map<Long, Long> allocations = new HashMap<>();

        for (long id : THREADS.getAllThreadIds())
            allocations.put(id, THREADS.getThreadAllocatedBytes(id));

        return allocations;
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

        private final long                              targetId;
        private final Map<Long, Long>                   cpuBefore;
        private final Map<Long, Long>                   allocBefore;
        private Map<Long, Long>                         allocAfter;
        private final Map<String, Integer>              self      = new HashMap<>();
        private final Map<String, Map<String, Integer>> callers   = new HashMap<>();
        private final Map<String, Integer>              inclusive = new HashMap<>();
        private final Set<String>                       seen      = new HashSet<>();
        private Map<Long, Long>                         cpuAfter;
        private double                                  seconds;
        private int                                     samples;
        private final List<Long>                        sampleTimes  = new ArrayList<>();
        private final List<StackTraceElement[]>         sampleStacks = new ArrayList<>();

        private Result(long targetId, Map<Long, Long> cpuBefore, Map<Long, Long> allocBefore) {
            this.targetId    = targetId;
            this.cpuBefore   = cpuBefore;
            this.allocBefore = allocBefore;
        }

        private void add(long sampledAt, StackTraceElement[] stack) {
            if (stack.length == 0)
                return;

            this.sampleTimes.add(sampledAt);
            this.sampleStacks.add(stack);

            this.samples ++;

            int    leafIndex = leafIndex(stack);
            String leaf      = frameName(stack[leafIndex]);

            this.self.merge(leaf, 1, Integer::sum);
            this.callers.computeIfAbsent(leaf, ignored -> new HashMap<>()).merge(this.caller(stack, leafIndex), 1, Integer::sum);
            this.seen.clear();

            for (StackTraceElement frame : stack) {
                String name = frameName(frame);

                if (this.seen.add(name))
                    this.inclusive.merge(name, 1, Integer::sum);
            }
        }

        private static int leafIndex(StackTraceElement[] stack) {
            int index = 0;

            while (index + 1 < stack.length && stack[index].getClassName().equals(NATIVE_TRAMPOLINE))
                index ++;

            return index;
        }

        private String caller(StackTraceElement[] stack, int leafIndex) {
            for (int index = leafIndex + 1; index < stack.length; index ++) {
                if (!isPlumbing(stack[index]))
                    return frameName(stack[index]);
            }

            return "<thread root>";
        }

        private void finish(Map<Long, Long> after, Map<Long, Long> allocations, double sampledSeconds) {
            this.cpuAfter   = after;
            this.allocAfter = allocations;
            this.seconds  = sampledSeconds;
        }

        private String report() {
            StringBuilder report = new StringBuilder();

            report.append(String.format(Locale.ROOT, "%nCPU: %d render thread samples over %.1f s%n%nRender thread self time%n", this.samples, this.seconds));
            this.self.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
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
                .forEach(entry -> report.append(String.format(Locale.ROOT, "%6.2f%%  %s%n", this.share(entry.getValue()), entry.getKey())));

            report.append(String.format(Locale.ROOT, "%nCPU time per thread (one core = 100%%)%n"));
            this.cpuAfter.entrySet().stream()
                .filter(entry -> entry.getValue() >= 0L && this.cpuBefore.containsKey(entry.getKey()))
                .map(entry -> Map.entry(entry.getKey(), entry.getValue() - this.cpuBefore.get(entry.getKey())))
                .sorted(Map.Entry.<Long, Long>comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> report.append(String.format(Locale.ROOT, "%6.1f%%  %s%n", entry.getValue() / 1.0e7 / this.seconds, threadName(entry.getKey()))));

            return report.toString();
        }

        private String slowFrameReport(long[] frameEnds, long[] frameTimes, int frameCount, List<GcPauses.Pause> pauses) {
            if (frameCount == 0)
                throw new IllegalStateException("No frames were recorded");

            long[] sorted = Arrays.copyOf(frameTimes, frameCount);

            Arrays.sort(sorted);

            long                              threshold  = sorted[Math.min(frameCount - 1, (int) Math.ceil(frameCount * SLOW_FRACTION) - 1)];
            Map<String, Integer>              slowSelf   = new HashMap<>();
            Map<String, Map<String, Integer>> slowCalls  = new HashMap<>();
            Map<String, Integer>              slowTotal  = new HashMap<>();
            Map<Integer, List<String>>        worstLeafs = new HashMap<>();
            Integer[]                         order      = new Integer[frameCount];
            int                               slowCount  = 0;
            int                               slowFrames = 0;

            for (int frame = 0; frame < frameCount; frame ++) {
                order[frame] = frame;

                if (frameTimes[frame] >= threshold)
                    slowFrames ++;
            }

            Arrays.sort(order, (first, second) -> Long.compare(frameTimes[second], frameTimes[first]));

            Set<Integer> worst = new HashSet<>();

            for (int index = 0; index < Math.min(WORST_FRAMES, frameCount); index ++)
                worst.add(order[index]);

            for (int sample = 0; sample < this.sampleTimes.size(); sample ++) {
                int frame = frameAt(frameEnds, frameTimes, frameCount, this.sampleTimes.get(sample));

                if (frame < 0 || frameTimes[frame] < threshold)
                    continue;

                StackTraceElement[] stack     = this.sampleStacks.get(sample);
                int                 leafIndex = leafIndex(stack);
                String              leaf      = frameName(stack[leafIndex]);
                String              caller    = this.caller(stack, leafIndex);

                slowCount ++;
                slowSelf.merge(leaf, 1, Integer::sum);
                slowCalls.computeIfAbsent(leaf, ignored -> new HashMap<>()).merge(caller, 1, Integer::sum);
                this.seen.clear();

                for (StackTraceElement element : stack) {
                    String name = frameName(element);

                    if (this.seen.add(name))
                        slowTotal.merge(name, 1, Integer::sum);
                }

                if (worst.contains(frame))
                    worstLeafs.computeIfAbsent(frame, ignored -> new ArrayList<>()).add(leaf + " <- " + caller);
            }

            StringBuilder report = new StringBuilder();
            int           total  = Math.max(slowCount, 1);

            long renderAllocated = this.allocAfter.get(this.targetId) - this.allocBefore.get(this.targetId);

            report.append(String.format(Locale.ROOT, "%nMemory allocated per thread (render thread %.1f KB per frame)%n", renderAllocated / 1024.0 / frameCount));
            this.allocAfter.entrySet().stream()
                .filter(entry -> entry.getValue() >= 0L && this.allocBefore.containsKey(entry.getKey()))
                .map(entry -> Map.entry(entry.getKey(), entry.getValue() - this.allocBefore.get(entry.getKey())))
                .filter(entry -> entry.getValue() > 0L)
                .sorted(Map.Entry.<Long, Long>comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> report.append(String.format(Locale.ROOT, "%8.1f MB/s  %s%n", entry.getValue() / 1048576.0 / this.seconds, threadName(entry.getKey()))));

            report.append(String.format(Locale.ROOT, "%nSlow frames: %d frames at or above %.3f ms (the slowest %.0f%%), %d samples inside them%n", slowFrames, threshold / 1.0e6, (1.0 - SLOW_FRACTION) * 100.0, slowCount));
            report.append(String.format(Locale.ROOT, "%nSlow frame self time%n"));
            slowSelf.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    report.append(String.format(Locale.ROOT, "%6.2f%%  %s%n", entry.getValue() * 100.0 / total, entry.getKey()));
                    slowCalls.get(entry.getKey()).entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(TOP_CALLERS)
                        .forEach(call -> report.append(String.format(Locale.ROOT, "          %6.2f%%  <- %s%n", call.getValue() * 100.0 / total, call.getKey())));
                });

            report.append(String.format(Locale.ROOT, "%nSlow frame total time including callees%n"));
            slowTotal.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> report.append(String.format(Locale.ROOT, "%6.2f%%  %s%n", entry.getValue() * 100.0 / total, entry.getKey())));

            report.append(String.format(Locale.ROOT, "%nWorst frames%n"));

            for (int index = 0; index < Math.min(WORST_FRAMES, frameCount); index ++) {
                int  frame = order[index];
                long end   = frameEnds[frame];
                long start = end - frameTimes[frame];

                report.append(String.format(Locale.ROOT, "%8.3f ms  at %.3f s%n", frameTimes[frame] / 1.0e6, (end - frameEnds[0]) / 1.0e9));

                for (GcPauses.Pause pause : pauses) {
                    if (pause.endNanos() >= start && pause.startNanos() <= end)
                        report.append(String.format(Locale.ROOT, "            overlaps GC %s, %.1f ms%n", pause.name(), (pause.endNanos() - pause.startNanos()) / 1.0e6));
                }

                for (String leaf : worstLeafs.getOrDefault(frame, List.of()))
                    report.append("            sample ").append(leaf).append('\n');
            }

            report.append(String.format(Locale.ROOT, "%nGarbage collections: %d%n", pauses.size()));

            for (GcPauses.Pause pause : pauses)
                report.append(String.format(Locale.ROOT, "%8.1f ms  at %.3f s  %s%n", (pause.endNanos() - pause.startNanos()) / 1.0e6, (pause.startNanos() - frameEnds[0]) / 1.0e9, pause.name()));

            return report.toString();
        }

        private static int frameAt(long[] frameEnds, long[] frameTimes, int frameCount, long time) {
            int low  = 0;
            int high = frameCount - 1;

            while (low < high) {
                int middle = (low + high) >>> 1;

                if (frameEnds[middle] < time)
                    low = middle + 1;
                else
                    high = middle;
            }

            if (frameEnds[low] < time || frameEnds[low] - frameTimes[low] > time)
                return -1;

            return low;
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
