package com.teslicek.txoptimizations.gpu;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import jdk.jfr.Recording;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public final class PassiveProfile {

    private static final Logger            LOGGER            = LogUtils.getLogger();
    private static final String            DIRECTORY         = "txoptimizations/passive";
    private static final String            FINISHED          = "[TxOptimizations] Background profiler finished";
    private static final String            PREFIX            = "passive-";
    private static final String            RECORDING         = ".jfr";
    private static final String            SUMMARY           = ".txt";
    private static final String            SAMPLE            = "jdk.ExecutionSample";
    private static final String            GC_PAUSE          = "jdk.GCPhasePause";
    private static final String            SAFEPOINT         = "jdk.SafepointBegin";
    private static final Duration          SAMPLE_PERIOD     = Duration.ofMillis(20);
    private static final Duration          LONG_PAUSE        = Duration.ofMillis(1);
    private static final Duration          KEEP_AGE          = Duration.ofMinutes(6);
    private static final long              WINDOW_NANOS      = Duration.ofMinutes(5).toNanos();
    private static final long              BUCKET_NANOS      = 50_000L;
    private static final int               BUCKETS           = 2000;
    private static final int               KEEP_FILES        = 24;
    private static final double[]          PERCENTILES       = {0.5, 0.9, 0.99, 0.999};
    private static final String[]          PERCENTILE_LABELS = {"p50", "p90", "p99", "p99.9"};
    private static final DateTimeFormatter FILE_TIME         = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");
    private static final int[]             HISTOGRAM         = new int[BUCKETS + 1];
    private static final ExecutorService   WRITER            = Executors.newSingleThreadExecutor(Thread.ofPlatform().name("TxOptimizations Passive Profile").daemon().factory());

    private static Recording recording;
    private static long      windowStart;
    private static long      windowNanos;
    private static long      longestNanos;
    private static int       windowFrames;

    private PassiveProfile() {
    }

    public static void start() {
        if (recording != null)
            throw new IllegalStateException("The passive profile is already recording");

        Recording started = new Recording();

        started.setName("TxOptimizations passive profile");
        started.enable(SAMPLE).withPeriod(SAMPLE_PERIOD);
        started.enable(GC_PAUSE).withThreshold(Duration.ZERO);
        started.enable(SAFEPOINT).withThreshold(LONG_PAUSE);
        started.setToDisk(true);
        started.setMaxAge(KEEP_AGE);
        started.start();
        recording = started;
    }

    public static void recordFrame(long frameNanos) {
        long now = System.nanoTime();

        if (windowStart == 0L) {
            windowStart = now;

            return;
        }

        windowFrames ++;
        windowNanos += frameNanos;
        longestNanos = Math.max(longestNanos, frameNanos);
        HISTOGRAM[(int) Math.min(frameNanos / BUCKET_NANOS, BUCKETS)] ++;

        if (now - windowStart < WINDOW_NANOS)
            return;

        Minecraft minecraft = Minecraft.getInstance();
        String    name      = PREFIX + LocalDateTime.now().format(FILE_TIME);
        String    summary   = summary(minecraft, name, (now - windowStart) / 1.0e9);
        Path      directory = minecraft.gameDirectory.toPath().resolve(DIRECTORY);

        Arrays.fill(HISTOGRAM, 0);
        windowStart  = now;
        windowNanos  = 0L;
        longestNanos = 0L;
        windowFrames = 0;

        WRITER.execute(() -> save(minecraft, directory, name, summary));
    }

    private static String summary(Minecraft minecraft, String name, double seconds) {
        StringBuilder summary = new StringBuilder(String.format(Locale.ROOT, "== %s: %d frames in %.1f s, %.0f fps, mean %.3f ms, max %.3f ms",
            name, windowFrames, seconds, windowFrames / seconds, windowNanos / 1.0e6 / windowFrames, longestNanos / 1.0e6));

        for (int index = 0; index < PERCENTILES.length; index ++)
            summary.append(String.format(Locale.ROOT, ", %s %.2f ms", PERCENTILE_LABELS[index], percentileMillis(PERCENTILES[index])));

        return summary.append(System.lineSeparator()).append(ProfileContext.capture(minecraft)).toString();
    }

    private static double percentileMillis(double percentile) {
        long target = (long) Math.ceil(windowFrames * percentile);
        long seen   = 0L;

        for (int bucket = 0; bucket < BUCKETS; bucket ++) {
            seen += HISTOGRAM[bucket];

            if (seen >= target)
                return (bucket + 1) * BUCKET_NANOS / 1.0e6;
        }

        return longestNanos / 1.0e6;
    }

    private static void save(Minecraft minecraft, Path directory, String name, String summary) {
        try {
            Files.createDirectories(directory);
            recording.dump(directory.resolve(name + RECORDING));
            Files.writeString(directory.resolve(name + SUMMARY), summary);
            prune(directory);
            minecraft.execute(() -> minecraft.gui.hud.getChat().addClientSystemMessage(Component.literal(FINISHED)));
        } catch (IOException exception) {
            LOGGER.error("Could not save the passive profile {} to {}", name, directory, exception);
        }
    }

    private static void prune(Path directory) throws IOException {
        List<Path> files;

        try (Stream<Path> listed = Files.list(directory)) {
            files = listed.filter(file -> file.getFileName().toString().startsWith(PREFIX)).sorted().toList();
        }

        List<String> names = files.stream().map(file -> file.getFileName().toString()).map(file -> file.substring(0, file.lastIndexOf('.'))).distinct().toList();

        if (names.size() <= KEEP_FILES)
            return;

        String oldestKept = names.get(names.size() - KEEP_FILES);

        for (Path file : files) {
            if (file.getFileName().toString().compareTo(oldestKept) < 0)
                Files.delete(file);
        }
    }
}
