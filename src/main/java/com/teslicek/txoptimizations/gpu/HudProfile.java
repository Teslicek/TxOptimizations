package com.teslicek.txoptimizations.gpu;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.navigation.ScreenRectangle;

public final class HudProfile {

    private static final int                 SHOWN   = 12;
    private static final Map<String, long[]> SOURCES = new HashMap<>();

    private HudProfile() {
    }

    static void reset() {
        SOURCES.clear();
    }

    public static void record(Object source, ScreenRectangle bounds) {
        if (!GpuPassProfiler.isRecording())
            return;

        long[] entry = SOURCES.computeIfAbsent(source.getClass().getName(), name -> new long[3]);

        entry[0] ++;

        if (bounds == null) {
            entry[1] ++;

            return;
        }

        entry[2] = Math.max(entry[2], (long) bounds.width() << 32 | bounds.height());
    }

    static String report() {
        StringBuilder report = new StringBuilder("HUD elements by type (count, without bounds, largest bounds in GUI pixels):");

        SOURCES.entrySet().stream()
            .sorted((first, second) -> Long.compare(area(second.getValue()[2]), area(first.getValue()[2])))
            .limit(SHOWN)
            .forEach(entry -> report.append(String.format(Locale.ROOT, "%n  %8d  %6d  %5dx%-5d  %s", entry.getValue()[0], entry.getValue()[1], entry.getValue()[2] >>> 32, entry.getValue()[2] & 0xFFFFFFFFL, entry.getKey())));

        return report.append(String.format(Locale.ROOT, "%n%n")).toString();
    }

    private static long area(long size) {
        return (size >>> 32) * (size & 0xFFFFFFFFL);
    }
}
