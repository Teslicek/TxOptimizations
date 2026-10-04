package com.teslicek.txoptimizations.gpu;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordingFile;

final class AllocationSampler {

    private static final String EVENT       = "jdk.ObjectAllocationSample";
    private static final int    SITE_FRAMES = 4;
    private static final int    TOP_SITES   = 40;
    private static final int    TOP_CLASSES = 15;

    private static Recording recording;

    private AllocationSampler() {
    }

    static void start() {
        if (recording != null)
            throw new IllegalStateException("Allocation sampler is already running");

        Recording started = new Recording();

        started.enable(EVENT).with("throttle", "5000/s").withStackTrace();
        started.start();
        recording = started;
    }

    static String finish(double seconds) {
        Recording finished = recording;

        if (finished == null)
            throw new IllegalStateException("Allocation sampler was never started");

        recording = null;
        finished.stop();

        try {
            Path file = Files.createTempFile("txprofile", ".jfr");

            try {
                finished.dump(file);

                return report(RecordingFile.readAllEvents(file), seconds);
            } finally {
                Files.delete(file);
                finished.close();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read the allocation recording", exception);
        }
    }

    private static String report(List<RecordedEvent> events, double seconds) {
        Map<String, Map<String, Long>> sitesByThread   = new HashMap<>();
        Map<String, Map<String, Long>> classesByThread = new HashMap<>();
        Map<String, Long>              threadTotals    = new HashMap<>();

        for (RecordedEvent event : events) {
            if (!event.getEventType().getName().equals(EVENT))
                continue;

            String thread = event.getThread().getJavaName();
            long   weight = event.getLong("weight");
            String type   = event.getClass("objectClass").getName();

            threadTotals.merge(thread, weight, Long::sum);
            classesByThread.computeIfAbsent(thread, ignored -> new HashMap<>()).merge(type, weight, Long::sum);
            sitesByThread.computeIfAbsent(thread, ignored -> new HashMap<>()).merge(type + "  " + site(event), weight, Long::sum);
        }

        StringBuilder report = new StringBuilder(String.format(Locale.ROOT, "%nAllocation sources (sampled by JDK Flight Recorder, MB/s)%n"));

        threadTotals.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .forEach(thread -> {
                report.append(String.format(Locale.ROOT, "%n%8.1f MB/s  %s%n", thread.getValue() / 1048576.0 / seconds, thread.getKey()));
                report.append("  by class\n");
                classesByThread.get(thread.getKey()).entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(TOP_CLASSES)
                    .forEach(entry -> report.append(String.format(Locale.ROOT, "  %8.2f MB/s  %s%n", entry.getValue() / 1048576.0 / seconds, entry.getKey())));
                report.append("  by allocation site\n");
                sitesByThread.get(thread.getKey()).entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(TOP_SITES)
                    .forEach(entry -> report.append(String.format(Locale.ROOT, "  %8.2f MB/s  %s%n", entry.getValue() / 1048576.0 / seconds, entry.getKey())));
            });

        return report.toString();
    }

    private static String site(RecordedEvent event) {
        if (event.getStackTrace() == null)
            return "<no stack>";

        StringBuilder site   = new StringBuilder();
        int           frames = 0;

        for (RecordedFrame frame : event.getStackTrace().getFrames()) {
            String className = frame.getMethod().getType().getName();

            if (frames == 0 && (className.startsWith("java.") || className.startsWith("jdk.")))
                continue;

            if (frames > 0)
                site.append(" <- ");

            site.append(className.substring(className.lastIndexOf('.') + 1)).append('.').append(frame.getMethod().getName());

            frames ++;

            if (frames == SITE_FRAMES)
                break;
        }

        return site.toString();
    }
}
