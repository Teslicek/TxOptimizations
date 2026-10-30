package com.teslicek.txoptimizations.gpu;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public final class ParticleProfile {

    private static final int                 SHOWN    = 8;
    private static final Map<String, long[]> BLOCKERS = new HashMap<>();

    private static long ahead;
    private static long few;
    private static long blocked;

    private ParticleProfile() {
    }

    static void reset() {
        BLOCKERS.clear();
        ahead   = 0L;
        few     = 0L;
        blocked = 0L;
    }

    public static void record(boolean wentAhead, boolean enough, @Nullable String blocker) {
        if (!GpuPassProfiler.isRecording())
            return;

        if (wentAhead) {
            ahead ++;
            return;
        }

        if (!enough) {
            few ++;
            return;
        }

        if (blocker == null)
            throw new IllegalStateException("Particles stayed on the render thread without a blocking particle");

        blocked ++;
        BLOCKERS.computeIfAbsent(blocker, name -> new long[1])[0] ++;
    }

    static String report() {
        StringBuilder report = new StringBuilder(String.format(Locale.ROOT, "Particle extraction frames: %d on the particle worker, %d on the render thread with too few particles, %d on the render thread blocked by:", ahead, few, blocked));

        BLOCKERS.entrySet().stream()
            .sorted((first, second) -> Long.compare(second.getValue()[0], first.getValue()[0]))
            .limit(SHOWN)
            .forEach(entry -> report.append(String.format(Locale.ROOT, "%n  %8d  %s", entry.getValue()[0], entry.getKey())));

        return report.append(String.format(Locale.ROOT, "%n%n")).toString();
    }
}
