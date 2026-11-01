package com.teslicek.txoptimizations.gpu;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class MainWaits {

    private static final int                 SHOWN   = 12;
    private static final int                 FRAMES  = 4;
    private static final String              WORKER  = "com.teslicek.txoptimizations.RenderWorker";
    private static final StackWalker         WALKER  = StackWalker.getInstance();
    private static final Map<String, long[]> CALLERS = new HashMap<>();

    private static long handOffs;
    private static long handOffNanos;

    private MainWaits() {
    }

    static void reset() {
        CALLERS.clear();
        handOffs     = 0L;
        handOffNanos = 0L;
    }

    public static void recordHandOff(long begin) {
        if (!GpuPassProfiler.isRecording())
            return;

        handOffs ++;
        handOffNanos += System.nanoTime() - begin;
    }

    public static void recordWait(long begin) {
        if (!GpuPassProfiler.isRecording())
            return;

        long   nanos  = System.nanoTime() - begin;
        String caller = WALKER.walk(frames -> frames.dropWhile(frame -> frame.getClassName().equals(WORKER) || frame.getClassName().equals(MainWaits.class.getName())).limit(FRAMES).map(frame -> frame.getClassName().substring(frame.getClassName().lastIndexOf('.') + 1) + "." + frame.getMethodName()).collect(Collectors.joining(" < ")));
        long[] entry  = CALLERS.computeIfAbsent(caller, key -> new long[2]);

        entry[0] ++;
        entry[1] += nanos;
    }

    static String report(int frames) {
        StringBuilder report = new StringBuilder(String.format(Locale.ROOT, "Main thread waited for the previous render job at %.1f%% of frame hand-offs, %.3f ms per frame%n", 100.0 * handOffs / frames, handOffNanos / 1.0e6 / frames));

        report.append("Main thread waits for the render job outside hand-offs (count, ms per frame):");

        CALLERS.entrySet().stream()
            .sorted((first, second) -> Long.compare(second.getValue()[1], first.getValue()[1]))
            .limit(SHOWN)
            .forEach(entry -> report.append(String.format(Locale.ROOT, "%n  %8d  %7.3f  %s", entry.getValue()[0], entry.getValue()[1] / 1.0e6 / frames, entry.getKey())));

        return report.append(String.format(Locale.ROOT, "%n%n")).toString();
    }
}
