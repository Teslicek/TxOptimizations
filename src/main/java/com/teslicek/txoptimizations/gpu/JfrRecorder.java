package com.teslicek.txoptimizations.gpu;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordingFile;

final class JfrRecorder {

    private static final String   ALLOCATION     = "jdk.ObjectAllocationSample";
    private static final String   SAFEPOINT      = "jdk.SafepointBegin";
    private static final String   COMPILATION    = "jdk.Compilation";
    private static final String   DEOPTIMIZATION = "jdk.Deoptimization";
    private static final String   PARK           = "jdk.ThreadPark";
    private static final String   MONITOR        = "jdk.JavaMonitorEnter";
    private static final Duration WAIT_THRESHOLD = Duration.ofNanos(200_000L);
    private static final int      SITE_FRAMES    = 6;
    private static final int      WAIT_FRAMES    = 6;
    private static final int      TOP_SITES      = 60;
    private static final int      TOP_CLASSES    = 25;
    private static final int      TOP_EVENTS     = 15;

    private static Recording recording;
    private static Instant   startTime;
    private static String    renderThread;

    private JfrRecorder() {
    }

    static void start(Thread target) {
        if (recording != null)
            throw new IllegalStateException("Flight Recorder profile is already running");

        Recording started = new Recording();

        started.enable(ALLOCATION).with("throttle", "5000/s").withStackTrace();
        started.enable(SAFEPOINT).withThreshold(Duration.ZERO);
        started.enable(COMPILATION).withThreshold(Duration.ZERO);
        started.enable(DEOPTIMIZATION).withStackTrace();
        started.enable(PARK).withThreshold(WAIT_THRESHOLD).withStackTrace();
        started.enable(MONITOR).withThreshold(WAIT_THRESHOLD).withStackTrace();
        started.start();
        recording    = started;
        startTime    = Instant.now();
        renderThread = target.getName();
    }

    static String finish(double seconds) {
        Recording finished = recording;

        if (finished == null)
            throw new IllegalStateException("Flight Recorder profile was never started");

        recording = null;
        finished.stop();

        try {
            Path file = Files.createTempFile("txprofile", ".jfr");

            try {
                finished.dump(file);

                List<RecordedEvent> events = RecordingFile.readAllEvents(file);

                return jvmReport(events) + allocationReport(events, seconds);
            } finally {
                Files.delete(file);
                finished.close();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read the Flight Recorder profile", exception);
        }
    }

    private static String jvmReport(List<RecordedEvent> events) {
        List<RecordedEvent> safepoints      = new ArrayList<>();
        List<RecordedEvent> compilations    = new ArrayList<>();
        List<RecordedEvent> waits           = new ArrayList<>();
        Map<String, Long>   deoptimizations = new HashMap<>();
        Map<String, Long>   waitNanos       = new HashMap<>();
        Map<String, Long>   waitCounts      = new HashMap<>();

        for (RecordedEvent event : events) {
            String type = event.getEventType().getName();

            switch (type) {
                case SAFEPOINT -> safepoints.add(event);
                case COMPILATION -> compilations.add(event);
                case DEOPTIMIZATION -> deoptimizations.merge(method(event.getValue("method")) + " (" + event.getString("reason") + ", " + event.getString("action") + ")", 1L, Long::sum);
                case PARK, MONITOR -> {
                    if (event.getThread() == null || !renderThread.equals(event.getThread().getJavaName()))
                        continue;

                    String site = (type.equals(PARK) ? "park  " : "lock  ") + frames(event, WAIT_FRAMES);

                    waits.add(event);
                    waitNanos.merge(site, event.getDuration().toNanos(), Long::sum);
                    waitCounts.merge(site, 1L, Long::sum);
                }
                default -> {
                }
            }
        }

        StringBuilder report = new StringBuilder(String.format(Locale.ROOT, "%nJVM events (JDK Flight Recorder)%n"));

        report.append(String.format(Locale.ROOT, "Safepoints: %d, %.2f ms in total%n", safepoints.size(), totalMillis(safepoints)));
        longest(safepoints).forEach(event -> report.append(String.format(Locale.ROOT, "  %8.3f ms  at %.3f s%n", millis(event), seconds(event))));

        report.append(String.format(Locale.ROOT, "JIT compilations: %d, %.1f ms of compiler time in total%n", compilations.size(), totalMillis(compilations)));
        longest(compilations).forEach(event -> report.append(String.format(Locale.ROOT, "  %8.3f ms  at %.3f s  tier %d  %s%n", millis(event), seconds(event), event.getInt("compileLevel"), method(event.getValue("method")))));

        report.append(String.format(Locale.ROOT, "Deoptimizations: %d%n", deoptimizations.values().stream().mapToLong(Long::longValue).sum()));
        deoptimizations.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(TOP_EVENTS)
            .forEach(entry -> report.append(String.format(Locale.ROOT, "  %6d  %s%n", entry.getValue(), entry.getKey())));

        report.append(String.format(Locale.ROOT, "Render thread waits of %.1f ms or longer: %d, %.2f ms in total%n", WAIT_THRESHOLD.toNanos() / 1.0e6, waits.size(), totalMillis(waits)));
        longest(waits).forEach(event -> report.append(String.format(Locale.ROOT, "  %8.3f ms  at %.3f s  %s%n", millis(event), seconds(event), frames(event, 2))));
        report.append(String.format(Locale.ROOT, "  by site%n"));
        waitNanos.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(TOP_EVENTS)
            .forEach(entry -> report.append(String.format(Locale.ROOT, "  %8.3f ms  %5dx  %s%n", entry.getValue() / 1.0e6, waitCounts.get(entry.getKey()), entry.getKey())));

        return report.toString();
    }

    private static String allocationReport(List<RecordedEvent> events, double seconds) {
        Map<String, Map<String, Long>> sitesByThread   = new HashMap<>();
        Map<String, Map<String, Long>> classesByThread = new HashMap<>();
        Map<String, Long>              threadTotals    = new HashMap<>();

        for (RecordedEvent event : events) {
            if (!event.getEventType().getName().equals(ALLOCATION))
                continue;

            String thread = event.getThread().getJavaName();
            long   weight = event.getLong("weight");
            String type   = event.getClass("objectClass").getName();

            threadTotals.merge(thread, weight, Long::sum);
            classesByThread.computeIfAbsent(thread, ignored -> new HashMap<>()).merge(type, weight, Long::sum);
            sitesByThread.computeIfAbsent(thread, ignored -> new HashMap<>()).merge(type + "  " + allocationSite(event), weight, Long::sum);
        }

        StringBuilder report = new StringBuilder(String.format(Locale.ROOT, "%nAllocation sources (sampled by JDK Flight Recorder, MB/s)%n"));

        threadTotals.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .forEach(thread -> {
                report.append(String.format(Locale.ROOT, "%n%8.1f MB/s  %s%n", thread.getValue() / 1048576.0 / seconds, thread.getKey()));
                report.append(String.format(Locale.ROOT, "  by class%n"));
                classesByThread.get(thread.getKey()).entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(TOP_CLASSES)
                    .forEach(entry -> report.append(String.format(Locale.ROOT, "  %8.2f MB/s  %s%n", entry.getValue() / 1048576.0 / seconds, entry.getKey())));
                report.append(String.format(Locale.ROOT, "  by allocation site%n"));
                sitesByThread.get(thread.getKey()).entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(TOP_SITES)
                    .forEach(entry -> report.append(String.format(Locale.ROOT, "  %8.2f MB/s  %s%n", entry.getValue() / 1048576.0 / seconds, entry.getKey())));
            });

        return report.toString();
    }

    private static List<RecordedEvent> longest(List<RecordedEvent> events) {
        return events.stream()
            .sorted(Comparator.comparing(RecordedEvent::getDuration, Comparator.reverseOrder()))
            .limit(TOP_EVENTS)
            .toList();
    }

    private static double totalMillis(List<RecordedEvent> events) {
        return events.stream().mapToLong(event -> event.getDuration().toNanos()).sum() / 1.0e6;
    }

    private static double millis(RecordedEvent event) {
        return event.getDuration().toNanos() / 1.0e6;
    }

    private static double seconds(RecordedEvent event) {
        return Duration.between(startTime, event.getStartTime()).toNanos() / 1.0e9;
    }

    private static String method(RecordedMethod method) {
        String className = method.getType().getName();

        return className.substring(className.lastIndexOf('.') + 1) + "." + method.getName();
    }

    private static String frames(RecordedEvent event, int limit) {
        if (event.getStackTrace() == null)
            return "<no stack>";

        StringBuilder site   = new StringBuilder();
        int           frames = 0;

        for (RecordedFrame frame : event.getStackTrace().getFrames()) {
            String className = frame.getMethod().getType().getName();

            if (className.startsWith("java.") || className.startsWith("jdk."))
                continue;

            if (frames > 0)
                site.append(" <- ");

            site.append(method(frame.getMethod()));
            frames ++;

            if (frames == limit)
                break;
        }

        return site.toString();
    }

    private static String allocationSite(RecordedEvent event) {
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

            site.append(method(frame.getMethod()));
            frames ++;

            if (frames == SITE_FRAMES)
                break;
        }

        return site.toString();
    }
}
