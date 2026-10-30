package com.teslicek.txoptimizations.gpu;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

public final class PassiveProfile {

    private static final String            DIRECTORY     = "txoptimizations/passive";
    private static final String            SUMMARY       = "summary.txt";
    private static final String            PREFIX        = "passive-";
    private static final String            EXTENSION     = ".jfr";
    private static final String            SAMPLE        = "jdk.ExecutionSample";
    private static final String            GC_PAUSE      = "jdk.GCPhasePause";
    private static final String            SAFEPOINT     = "jdk.SafepointBegin";
    private static final Duration          SAMPLE_PERIOD = Duration.ofMillis(20);
    private static final Duration          LONG_PAUSE    = Duration.ofMillis(1);
    private static final Duration          KEEP_AGE      = Duration.ofMinutes(6);
    private static final long              WINDOW_NANOS  = Duration.ofMinutes(5).toNanos();
    private static final long              BUCKET_NANOS  = 50_000L;
    private static final int               BUCKETS       = 2000;
    private static final int               KEEP_FILES    = 24;
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
        String    name      = PREFIX + LocalDateTime.now().format(FILE_TIME) + EXTENSION;
        String    summary   = summary(minecraft, name, (now - windowStart) / 1.0e9);
        Path      directory = minecraft.gameDirectory.toPath().resolve(DIRECTORY);

        Arrays.fill(HISTOGRAM, 0);
        windowStart  = now;
        windowNanos  = 0L;
        longestNanos = 0L;
        windowFrames = 0;

        WRITER.execute(() -> save(directory, name, summary));
    }

    private static String summary(Minecraft minecraft, String name, double seconds) {
        StringBuilder summary = new StringBuilder(String.format(Locale.ROOT, "== %s: %d frames in %.1f s, %.0f fps, mean %.3f ms, max %.3f ms",
            name, windowFrames, seconds, windowFrames / seconds, windowNanos / 1.0e6 / windowFrames, longestNanos / 1.0e6));

        for (int index = 0; index < PERCENTILES.length; index ++)
            summary.append(String.format(Locale.ROOT, ", %s %.2f ms", PERCENTILE_LABELS[index], percentileMillis(PERCENTILES[index])));

        return summary.append(System.lineSeparator()).append(ProfileContext.capture(minecraft)).append(System.lineSeparator()).toString();
    }

    private static double percentileMillis(double percentile) {
        long target = (long) Math.ceil(windowFrames * percentile);
        long seen   = 0L;

        for (int bucket = 0; bucket <= BUCKETS; bucket ++) {
            seen += HISTOGRAM[bucket];

            if (seen >= target)
                return (bucket + 1) * BUCKET_NANOS / 1.0e6;
        }

        throw new IllegalStateException("The frame histogram holds " + seen + " frames but the window counted " + windowFrames);
    }

    private static void save(Path directory, String name, String summary) {
        try {
            Files.createDirectories(directory);
            recording.dump(directory.resolve(name));
            Files.writeString(directory.resolve(SUMMARY), summary, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            prune(directory);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not save the passive profile " + name + " to " + directory, exception);
        }
    }

    private static void prune(Path directory) throws IOException {
        List<Path> recordings;

        try (Stream<Path> files = Files.list(directory)) {
            recordings = files.filter(file -> file.getFileName().toString().startsWith(PREFIX) && file.getFileName().toString().endsWith(EXTENSION)).sorted().toList();
        }

        for (int index = 0; index < recordings.size() - KEEP_FILES; index ++)
            Files.delete(recordings.get(index));
    }
}
