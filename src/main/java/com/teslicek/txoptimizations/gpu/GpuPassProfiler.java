package com.teslicek.txoptimizations.gpu;

import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.backend.api.CommandEncoderBackend;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class GpuPassProfiler {

    private static final int               SLOTS      = 3;
    private static final int               CAPACITY   = 4096;
    private static final long              DURATION   = 10_000_000_000L;
    private static final int               CHAT_LINES = 8;
    private static final String            FRAME_END  = "frame end";
    private static final DateTimeFormatter FILE_TIME  = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");
    private static final GpuQueryPool[]    POOLS      = new GpuQueryPool[SLOTS];
    private static final String[][]        LABELS     = new String[SLOTS][CAPACITY];
    private static final int[]             COUNTS     = new int[SLOTS];
    private static final Map<String, Pass> PASSES     = new HashMap<>();

    private static int     slot;
    private static boolean recording;
    private static int     framesPending;
    private static int     framesRead;
    private static long    gpuNanos;
    private static long    startNanos;
    private static double  nanosPerTick;

    private GpuPassProfiler() {
    }

    public static void start() {
        if (isRunning())
            throw new IllegalStateException("GPU profile is already running");

        PASSES.clear();
        recording  = true;
        framesRead = 0;
        gpuNanos   = 0L;
        startNanos = System.nanoTime();
    }

    public static boolean isRunning() {
        return recording || framesPending > 0;
    }

    public static void mark(VulkanDevice device, CommandEncoderBackend encoder, String label) {
        if (!recording)
            return;

        if (POOLS[0] == null)
            openPools(device);

        int index = COUNTS[slot];

        if (index == CAPACITY)
            throw new IllegalStateException("GPU profile recorded more than " + CAPACITY + " passes in one frame");

        encoder.writeTimestamp(POOLS[slot], index);
        LABELS[slot][index] = label;
        COUNTS[slot]        = index + 1;
    }

    public static void endFrame(VulkanDevice device, CommandEncoderBackend encoder) {
        if (!isRunning())
            return;

        if (recording) {
            mark(device, encoder, FRAME_END);
            framesPending ++;
            recording = System.nanoTime() - startNanos < DURATION;
        }

        slot = (slot + 1) % SLOTS;

        if (COUNTS[slot] > 0) {
            read(slot);
            framesPending --;
        }

        if (!isRunning())
            finish();
    }

    private static void openPools(VulkanDevice device) {
        nanosPerTick = device.getDeviceInfo().timestampPeriod();

        for (int index = 0; index < SLOTS; index ++)
            POOLS[index] = device.createTimestampQueryPool(CAPACITY);
    }

    private static void read(int frameSlot) {
        int            count  = COUNTS[frameSlot];
        OptionalLong[] values = POOLS[frameSlot].getValues(0, count);
        long[]         ticks  = new long[count];

        for (int index = 0; index < count; index ++) {
            if (values[index].isEmpty())
                throw new IllegalStateException("GPU timestamp " + index + " of " + LABELS[frameSlot][index] + " is not available after its frame completed");

            ticks[index] = values[index].getAsLong();
        }

        for (int index = 0; index + 1 < count; index ++)
            PASSES.computeIfAbsent(LABELS[frameSlot][index], label -> new Pass()).add(nanos(ticks[index + 1] - ticks[index]));

        gpuNanos          += nanos(ticks[count - 1] - ticks[0]);
        framesRead        ++;
        COUNTS[frameSlot]  = 0;
    }

    private static long nanos(long ticks) {
        return Math.round(ticks * nanosPerTick);
    }

    private static void finish() {
        for (int index = 0; index < SLOTS; index ++) {
            POOLS[index].close();
            POOLS[index] = null;
        }

        double seconds = (System.nanoTime() - startNanos) / 1.0e9;
        String report  = report(seconds);
        Path   file    = Minecraft.getInstance().gameDirectory.toPath().resolve("logs").resolve("txgpu-" + LocalDateTime.now().format(FILE_TIME) + ".txt");

        try {
            Files.writeString(file, report);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not write GPU profile to " + file, exception);
        }

        String[]      lines = report.split("\n");
        StringBuilder chat  = new StringBuilder("GPU profile saved to logs/" + file.getFileName());

        for (int index = 0; index < Math.min(CHAT_LINES, lines.length); index ++)
            chat.append("\n").append(lines[index]);

        Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(Component.literal(chat.toString()));
    }

    private static String report(double seconds) {
        double        frameMs = gpuNanos / 1.0e6 / framesRead;
        StringBuilder report  = new StringBuilder();

        report.append(String.format(Locale.ROOT, "%d frames in %.1f s (%.0f fps), GPU %.3f ms per frame%n", framesRead, seconds, framesRead / seconds, frameMs));

        PASSES.entrySet().stream()
            .sorted((first, second) -> Long.compare(second.getValue().nanos, first.getValue().nanos))
            .forEach(entry -> report.append(String.format(Locale.ROOT, "%6.2f%%  %7.3f ms  %6.1fx  %s%n", entry.getValue().nanos * 100.0 / gpuNanos, entry.getValue().nanos / 1.0e6 / framesRead, (double) entry.getValue().calls / framesRead, entry.getKey())));

        return report.toString();
    }

    private static final class Pass {

        private long nanos;
        private long calls;

        private void add(long passNanos) {
            this.nanos += passNanos;
            this.calls ++;
        }
    }
}
