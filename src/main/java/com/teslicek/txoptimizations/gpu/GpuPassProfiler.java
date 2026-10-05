package com.teslicek.txoptimizations.gpu;

import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.backend.api.CommandEncoderBackend;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanQueryPool;
import com.mojang.renderpearl.frontend.FrontendRenderPipeline;
import com.teslicek.txoptimizations.FramePath;
import com.teslicek.txoptimizations.mixin.gpu.VulkanQueryPoolAccessor;
import org.lwjgl.vulkan.VK12;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;

public final class GpuPassProfiler {

    private static final int                                 SLOTS           = 3;
    private static final int                                 CAPACITY        = 4096;
    private static final long                                DURATION        = 10_000_000_000L;
    private static final int                                 CHAT_LINES      = 8;
    private static final int                                 SLOW_FRAMES     = 10;
    private static final int                                 SLOW_PASSES     = 4;
    private static final long[]                              FRAME_LIMITS    = {1_000_000L, 2_000_000L, 4_000_000L, 8_000_000L, 16_000_000L};
    private static final String                              FRAME_END       = "frame end";
    private static final GpuQueryPool[]                      POOLS           = new GpuQueryPool[SLOTS];
    private static final String[][]                          LABELS          = new String[SLOTS][CAPACITY];
    private static final int[]                               COUNTS          = new int[SLOTS];
    private static final Map<String, Pass>                   PASSES          = new HashMap<>();
    private static final Map<Object, String>                 COPY_LABELS     = new IdentityHashMap<>();
    private static final Map<FrontendRenderPipeline, String> PIPELINE_LABELS = new IdentityHashMap<>();
    private static final long[]                              SLOW_NANOS      = new long[SLOW_FRAMES];
    private static final String[]                            SLOW_TEXTS      = new String[SLOW_FRAMES];

    private static VulkanDevice          device;
    private static CommandEncoderBackend encoder;
    private static int                   slot;
    private static boolean               recording;
    private static int                   framesPending;
    private static int                   framesRead;
    private static long                  gpuNanos;
    private static long                  startNanos;
    private static long                  measuredFrom;
    private static double                nanosPerTick;
    private static long                  terrainDraws;
    private static long                  terrainIndices;
    private static long[]                frameTimes = new long[1 << 16];
    private static long[]                frameEnds  = new long[1 << 16];
    private static int                   frameTimeCount;
    private static String                context;
    private static String                screenFormats;

    private GpuPassProfiler() {
    }

    public static void start() {
        if (isRunning())
            throw new IllegalStateException("GPU profile is already running");

        PASSES.clear();
        COPY_LABELS.clear();
        PIPELINE_LABELS.clear();
        recording      = true;
        framesRead     = 0;
        gpuNanos       = 0L;
        terrainDraws   = 0L;
        terrainIndices = 0L;
        startNanos     = System.nanoTime();
        Arrays.fill(SLOW_NANOS, 0L);
        Arrays.fill(SLOW_TEXTS, null);
        FramePath.resetCounts();
        ProfileCounters.reset();
        context        = ProfileContext.capture(Minecraft.getInstance());
        frameTimeCount = 0;
        screenFormats  = null;
        GcPauses.start();
        JfrRecorder.start(Thread.currentThread());
        CpuSampler.start(Thread.currentThread(), DURATION);
        measuredFrom   = System.nanoTime();
    }

    public static boolean isRunning() {
        return recording || framesPending > 0;
    }

    public static boolean isRecording() {
        return recording;
    }

    public static void recordFrameTime(long frameNanos) {
        if (!recording)
            return;

        long now = System.nanoTime();

        if (now - frameNanos < measuredFrom)
            return;

        if (frameTimeCount == frameTimes.length) {
            frameTimes = Arrays.copyOf(frameTimes, frameTimes.length * 2);
            frameEnds  = Arrays.copyOf(frameEnds, frameEnds.length * 2);
        }

        frameTimes[frameTimeCount] = frameNanos;
        frameEnds[frameTimeCount]  = now;
        frameTimeCount ++;
    }

    public static void countTerrainDraws(int draws, long indices) {
        terrainDraws   += draws;
        terrainIndices += indices;
    }

    public static void mark(VulkanDevice frameDevice, CommandEncoderBackend frameEncoder, String label) {
        if (!recording)
            return;

        device  = frameDevice;
        encoder = frameEncoder;

        if (POOLS[0] == null)
            openPools(frameDevice);

        int index = COUNTS[slot];

        if (index == CAPACITY)
            throw new IllegalStateException("GPU profile recorded more than " + CAPACITY + " passes in one frame");

        frameEncoder.writeTimestamp(POOLS[slot], index);
        LABELS[slot][index] = label;
        COUNTS[slot]        = index + 1;
    }

    public static void recordScreenFormats(int swapchainFormat, Object framebufferFormat) {
        if (screenFormats == null)
            screenFormats = String.format(Locale.ROOT, "Screen: swapchain %s, main framebuffer %s%n", swapchainFormat(swapchainFormat), framebufferFormat);
    }

    private static String swapchainFormat(int format) {
        return switch (format) {
            case VK12.VK_FORMAT_R8G8B8A8_UNORM -> "R8G8B8A8_UNORM";
            case VK12.VK_FORMAT_B8G8R8A8_UNORM -> "B8G8R8A8_UNORM";
            default -> "Vulkan format " + format;
        };
    }

    public static String copyLabel(Object target) {
        return COPY_LABELS.computeIfAbsent(target, ignored -> copyLabel());
    }

    public static boolean ownsPool(GpuQueryPool pool) {
        for (GpuQueryPool owned : POOLS) {
            if (owned == pool)
                return true;
        }

        return false;
    }

    public static String copyLabel() {
        return "copy from " + StackWalker.getInstance().walk(frames -> frames
            .filter(frame -> !frame.getClassName().startsWith("com.mojang.renderpearl.") && !frame.getClassName().startsWith("com.teslicek.txoptimizations.") && !frame.getClassName().contains("$$Lambda"))
            .findFirst()
            .map(frame -> frame.getClassName().substring(frame.getClassName().lastIndexOf('.') + 1) + "." + frame.getMethodName())
            .orElseThrow(() -> new IllegalStateException("A GPU copy has no caller outside the renderer")));
    }

    public static void markPipeline(FrontendRenderPipeline pipeline) {
        if (!recording || encoder == null)
            return;

        String label = PIPELINE_LABELS.computeIfAbsent(pipeline, key -> "pipeline " + key.name());
        int    count = COUNTS[slot];

        if (count > 0 && LABELS[slot][count - 1].equals(label))
            return;

        mark(device, encoder, label);
    }

    public static void endFrame(VulkanDevice frameDevice, CommandEncoderBackend frameEncoder) {
        if (!isRunning())
            return;

        if (recording) {
            mark(frameDevice, frameEncoder, FRAME_END);
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

    private static void openPools(VulkanDevice poolDevice) {
        nanosPerTick = poolDevice.getDeviceInfo().timestampPeriod();

        for (int index = 0; index < SLOTS; index ++) {
            POOLS[index] = poolDevice.createTimestampQueryPool(CAPACITY);
            resetQueries(poolDevice, POOLS[index], CAPACITY);
        }
    }

    private static void resetQueries(VulkanDevice poolDevice, GpuQueryPool pool, int count) {
        VK12.vkResetQueryPool(poolDevice.vkDevice(), ((VulkanQueryPoolAccessor) (VulkanQueryPool) pool).txoptimizations$vkQueryPool(), 0, count);
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

        resetQueries(device, POOLS[frameSlot], count);
        recordSlowFrame(frameSlot, ticks, count, nanos(ticks[count - 1] - ticks[0]));
        gpuNanos          += nanos(ticks[count - 1] - ticks[0]);
        framesRead        ++;
        COUNTS[frameSlot]  = 0;
    }

    private static void recordSlowFrame(int frameSlot, long[] ticks, int count, long frameNanos) {
        int fastest = 0;

        for (int index = 1; index < SLOW_FRAMES; index ++) {
            if (SLOW_NANOS[index] < SLOW_NANOS[fastest])
                fastest = index;
        }

        if (frameNanos <= SLOW_NANOS[fastest])
            return;

        Map<String, Long> frame = new HashMap<>();

        for (int index = 0; index + 1 < count; index ++)
            frame.merge(LABELS[frameSlot][index], nanos(ticks[index + 1] - ticks[index]), Long::sum);

        StringBuilder text = new StringBuilder();

        frame.entrySet().stream()
            .sorted((first, second) -> Long.compare(second.getValue(), first.getValue()))
            .limit(SLOW_PASSES)
            .forEach(entry -> text.append(String.format(Locale.ROOT, "%n            %7.3f ms  %s", entry.getValue() / 1.0e6, entry.getKey())));

        SLOW_NANOS[fastest] = frameNanos;
        SLOW_TEXTS[fastest] = String.format(Locale.ROOT, "%8.3f ms GPU at %.3f s", frameNanos / 1.0e6, (System.nanoTime() - startNanos) / 1.0e9) + text;
    }

    private static String slowFrameReport() {
        StringBuilder report = new StringBuilder("Slowest GPU frames (read when their frame completed)" + System.lineSeparator());
        Integer[]     order  = new Integer[SLOW_FRAMES];

        for (int index = 0; index < SLOW_FRAMES; index ++)
            order[index] = index;

        Arrays.sort(order, (first, second) -> Long.compare(SLOW_NANOS[second], SLOW_NANOS[first]));

        for (int index : order) {
            if (SLOW_TEXTS[index] != null)
                report.append(SLOW_TEXTS[index]).append(System.lineSeparator());
        }

        return report.append(System.lineSeparator()).toString();
    }

    private static long nanos(long ticks) {
        return Math.round(ticks * nanosPerTick);
    }

    private static void finish() {
        for (int index = 0; index < SLOTS; index ++) {
            POOLS[index].close();
            POOLS[index] = null;
        }

        double           seconds = (System.nanoTime() - startNanos) / 1.0e9;
        String           gpu     = report(seconds);
        Path             file    = ReportFiles.write("txprofile", gpu + CpuSampler.finish(frameEnds, frameTimes, frameTimeCount, GcPauses.finish()) + JfrRecorder.finish(seconds));
        String[]         lines   = gpu.split("\n");
        MutableComponent chat    = ReportFiles.savedMessage("Profile", file);

        for (int index = 0; index < Math.min(CHAT_LINES, lines.length); index ++)
            chat.append("\n" + lines[index]);

        Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(chat);
    }

    private static String report(double seconds) {
        double        frameMs = gpuNanos / 1.0e6 / framesRead;
        StringBuilder report  = new StringBuilder();

        report.append(String.format(Locale.ROOT, "%d frames in %.1f s (%.0f fps), GPU %.3f ms per frame, GPU busy %.0f%% of the time%n", framesRead, seconds, framesRead / seconds, frameMs, gpuNanos / 1.0e7 / seconds));
        report.append(String.format(Locale.ROOT, "Present thread used for %.0f%% of frames%n", FramePath.workerShare() * 100.0));
        report.append(frameTimeReport());
        report.append(String.format(Locale.ROOT, "Terrain %.0f draws and %.3f million triangles per frame%n", (double) terrainDraws / framesRead, terrainIndices / 3.0 / 1.0e6 / framesRead));
        report.append(ProfileCounters.report(framesRead));
        report.append(context);

        if (screenFormats == null)
            throw new IllegalStateException("GPU profile never copied a frame to the screen");

        report.append(screenFormats);
        report.append(frameDistributionReport());

        PASSES.entrySet().stream()
            .sorted((first, second) -> Long.compare(second.getValue().nanos, first.getValue().nanos))
            .forEach(entry -> report.append(String.format(Locale.ROOT, "%6.2f%%  %7.3f ms  %6.1fx  %s%n", entry.getValue().nanos * 100.0 / gpuNanos, entry.getValue().nanos / 1.0e6 / framesRead, (double) entry.getValue().calls / framesRead, entry.getKey())));

        report.append(System.lineSeparator()).append(slowFrameReport());

        return report.toString();
    }

    private static String frameTimeReport() {
        if (frameTimeCount == 0)
            throw new IllegalStateException("No frame times were recorded");

        long[] sorted = Arrays.copyOf(frameTimes, frameTimeCount);
        long   total  = 0L;

        Arrays.sort(sorted);

        for (long frame : sorted)
            total += frame;

        return String.format(Locale.ROOT, "Frame times: average %.0f fps, 1%% low %.0f fps, 0.1%% low %.0f fps, worst frame %.2f ms%n", frameTimeCount / (total / 1.0e9), 1.0e9 / percentile(sorted, 0.99), 1.0e9 / percentile(sorted, 0.999), sorted[sorted.length - 1] / 1.0e6);
    }

    private static String frameDistributionReport() {
        long[] sorted = Arrays.copyOf(frameTimes, frameTimeCount);

        Arrays.sort(sorted);

        StringBuilder report = new StringBuilder(String.format(Locale.ROOT, "Frame time percentiles: p50 %.3f ms, p90 %.3f ms, p99 %.3f ms, p99.9 %.3f ms, max %.3f ms%nFrames over", percentile(sorted, 0.5) / 1.0e6, percentile(sorted, 0.9) / 1.0e6, percentile(sorted, 0.99) / 1.0e6, percentile(sorted, 0.999) / 1.0e6, sorted[sorted.length - 1] / 1.0e6));

        for (long limit : FRAME_LIMITS) {
            int over = 0;

            for (long frame : sorted) {
                if (frame > limit)
                    over ++;
            }

            report.append(String.format(Locale.ROOT, " %d ms: %d,", limit / 1_000_000L, over));
        }

        report.setLength(report.length() - 1);
        report.append(String.format(Locale.ROOT, "%nPer second (frames, average fps, worst frame):"));

        int  frame  = 0;
        long origin = frameEnds[0] - frameTimes[0];

        for (int second = 0; frame < frameTimeCount; second ++) {
            long end    = origin + (second + 1) * 1_000_000_000L;
            long total  = 0L;
            long worst  = 0L;
            int  frames = 0;

            while (frame < frameTimeCount && frameEnds[frame] <= end) {
                total += frameTimes[frame];
                worst  = Math.max(worst, frameTimes[frame]);
                frames ++;
                frame ++;
            }

            if (frames > 0)
                report.append(String.format(Locale.ROOT, "%n  %2d s  %6d  %7.0f fps  %7.3f ms", second, frames, frames / (total / 1.0e9), worst / 1.0e6));
        }

        return report.append(String.format(Locale.ROOT, "%n%n")).toString();
    }

    private static long percentile(long[] sorted, double fraction) {
        return sorted[Math.min(sorted.length - 1, (int) Math.ceil(sorted.length * fraction) - 1)];
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
