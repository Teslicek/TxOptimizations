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
import java.util.TreeMap;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.consumer.RecordingFile;

final class JfrRecorder {

    private static final String   ALLOCATION     = "jdk.ObjectAllocationSample";
    private static final String   SAFEPOINT      = "jdk.SafepointBegin";
    private static final String   COMPILATION    = "jdk.Compilation";
    private static final String   DEOPTIMIZATION = "jdk.Deoptimization";
    private static final String   PARK           = "jdk.ThreadPark";
    private static final String   MONITOR        = "jdk.JavaMonitorEnter";
    private static final String   VM_OPERATION   = "jdk.ExecuteVMOperation";
    private static final String   GC_PAUSE       = "jdk.GCPhasePause";
    private static final String   GC_PHASE       = "jdk.GCPhasePauseLevel1";
    private static final String   GC_WORKER      = "jdk.GCPhaseParallel";
    private static final String   GC_EVACUATION  = "jdk.EvacuationInformation";
    private static final String   GC_HEAP        = "jdk.G1HeapSummary";
    private static final String   BEFORE_GC      = "Before GC";
    private static final String   OLD_OBJECT     = "jdk.OldObjectSample";
    private static final Duration WAIT_THRESHOLD = Duration.ofNanos(200_000L);
    private static final int      SITE_FRAMES    = 6;
    private static final int      WAIT_FRAMES    = 6;
    private static final int      TOP_SITES      = 60;
    private static final int      TOP_CLASSES    = 25;
    private static final int      TOP_EVENTS     = 15;
    private static final int      TOP_WORKERS    = 8;
    private static final int      TOP_SURVIVORS  = 40;

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
        started.enable(VM_OPERATION).withThreshold(Duration.ZERO);
        started.enable(GC_PAUSE).withThreshold(Duration.ZERO);
        started.enable(GC_PHASE).withThreshold(Duration.ZERO);
        started.enable(GC_WORKER).withThreshold(Duration.ZERO);
        started.enable(GC_EVACUATION);
        started.enable(GC_HEAP);
        started.enable(OLD_OBJECT).with("cutoff", "0 ns").withStackTrace();
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

                return jvmReport(events) + gcReport(events) + survivorReport(events) + allocationReport(events, seconds);
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
        List<RecordedEvent> operations      = new ArrayList<>();
        Map<String, Long>   operationNanos  = new HashMap<>();
        Map<String, Long>   operationCounts = new HashMap<>();
        Map<String, Long>   deoptimizations = new HashMap<>();
        Map<String, Long>   waitNanos       = new HashMap<>();
        Map<String, Long>   waitCounts      = new HashMap<>();

        for (RecordedEvent event : events) {
            String type = event.getEventType().getName();

            switch (type) {
                case SAFEPOINT -> safepoints.add(event);
                case COMPILATION -> compilations.add(event);
                case VM_OPERATION -> {
                    String name = event.getString("operation") + (event.getBoolean("safepoint") ? " (safepoint)" : "");

                    operations.add(event);
                    operationNanos.merge(name, event.getDuration().toNanos(), Long::sum);
                    operationCounts.merge(name, 1L, Long::sum);
                }
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

        report.append(String.format(Locale.ROOT, "VM operations: %d, %.2f ms in total%n", operations.size(), totalMillis(operations)));
        longest(operations).forEach(event -> report.append(String.format(Locale.ROOT, "  %8.3f ms  at %.3f s  %s%s%n", millis(event), seconds(event), event.getString("operation"), event.getBoolean("safepoint") ? " (safepoint)" : "")));
        report.append(String.format(Locale.ROOT, "  by operation%n"));
        operationNanos.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(TOP_EVENTS)
            .forEach(entry -> report.append(String.format(Locale.ROOT, "  %8.3f ms  %5dx  %s%n", entry.getValue() / 1.0e6, operationCounts.get(entry.getKey()), entry.getKey())));

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

    private static String gcReport(List<RecordedEvent> events) {
        Map<Integer, RecordedEvent>       pauses      = new TreeMap<>();
        Map<Integer, List<RecordedEvent>> phases      = new HashMap<>();
        Map<Integer, Map<String, Long>>   workers     = new HashMap<>();
        Map<Integer, RecordedEvent>       evacuations = new HashMap<>();
        Map<Integer, RecordedEvent>       heapBefore  = new HashMap<>();
        Map<Integer, RecordedEvent>       heapAfter   = new HashMap<>();

        for (RecordedEvent event : events) {
            switch (event.getEventType().getName()) {
                case GC_PAUSE -> pauses.put(event.getInt("gcId"), event);
                case GC_PHASE -> phases.computeIfAbsent(event.getInt("gcId"), ignored -> new ArrayList<>()).add(event);
                case GC_WORKER -> workers.computeIfAbsent(event.getInt("gcId"), ignored -> new HashMap<>()).merge(event.getString("name"), event.getDuration().toNanos(), Math::max);
                case GC_EVACUATION -> evacuations.put(event.getInt("gcId"), event);
                case GC_HEAP -> (BEFORE_GC.equals(event.getString("when")) ? heapBefore : heapAfter).put(event.getInt("gcId"), event);
                default -> {
                }
            }
        }

        StringBuilder report = new StringBuilder(String.format(Locale.ROOT, "%nGarbage collection pauses (G1 phases, sizes in MB)%n"));

        if (pauses.isEmpty())
            report.append(String.format(Locale.ROOT, "  none%n"));

        pauses.forEach((gcId, pause) -> {
            report.append(String.format(Locale.ROOT, "  gc %d at %.3f s: %s, %.3f ms%n", gcId, seconds(pause), pause.getString("name"), millis(pause)));

            RecordedEvent before = heapBefore.get(gcId);
            RecordedEvent after  = heapAfter.get(gcId);

            if (before != null && after != null)
                report.append(String.format(Locale.ROOT, "    eden %.1f -> %.1f, survivors %.1f -> %.1f, old %.1f -> %.1f, regions %d%n", megabytes(before, "edenUsedSize"), megabytes(after, "edenUsedSize"), megabytes(before, "survivorUsedSize"), megabytes(after, "survivorUsedSize"), megabytes(before, "oldGenUsedSize"), megabytes(after, "oldGenUsedSize"), after.getInt("numberOfRegions")));

            RecordedEvent evacuation = evacuations.get(gcId);

            if (evacuation != null)
                report.append(String.format(Locale.ROOT, "    collection set %d regions, %.1f MB used, %.2f MB copied, %d regions freed%n", evacuation.getInt("cSetRegions"), megabytes(evacuation, "cSetUsedBefore"), megabytes(evacuation, "bytesCopied"), evacuation.getInt("regionsFreed")));

            phases.getOrDefault(gcId, List.of()).stream()
                .sorted(Comparator.comparing(RecordedEvent::getDuration, Comparator.reverseOrder()))
                .forEach(phase -> report.append(String.format(Locale.ROOT, "    %8.3f ms  %s%n", millis(phase), phase.getString("name"))));

            workers.getOrDefault(gcId, Map.of()).entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_WORKERS)
                .forEach(entry -> report.append(String.format(Locale.ROOT, "    %8.3f ms  slowest worker in %s%n", entry.getValue() / 1.0e6, entry.getKey())));
        });

        return report.toString();
    }

    private static String survivorReport(List<RecordedEvent> events) {
        Map<String, Long> counts  = new HashMap<>();
        Map<String, Long> ages    = new HashMap<>();
        Map<String, Long> sizes   = new HashMap<>();
        int               samples = 0;

        for (RecordedEvent event : events) {
            if (!event.getEventType().getName().equals(OLD_OBJECT))
                continue;

            RecordedObject object = event.getValue("object");

            if (object == null)
                throw new IllegalStateException("Old object sample has no object: " + event);

            String site = object.getClass("type").getName() + "  " + allocationSite(event);

            counts.merge(site, 1L, Long::sum);
            ages.merge(site, event.getDuration("objectAge").toNanos(), Long::sum);
            sizes.merge(site, event.getLong("objectSize"), Long::sum);
            samples ++;
        }

        StringBuilder report = new StringBuilder(String.format(Locale.ROOT, "%nObjects still alive when the profile ended (Flight Recorder old object samples, %d samples)%n", samples));

        counts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(TOP_SURVIVORS)
            .forEach(entry -> report.append(String.format(Locale.ROOT, "  %4dx  %6.2f s old on average  %9.1f KB sampled  %s%n", entry.getValue(), ages.get(entry.getKey()) / 1.0e9 / entry.getValue(), sizes.get(entry.getKey()) / 1024.0, entry.getKey())));

        return report.toString();
    }

    private static double megabytes(RecordedEvent event, String field) {
        return event.getLong(field) / 1048576.0;
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
