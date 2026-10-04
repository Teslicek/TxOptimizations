package com.teslicek.txoptimizations.gpu;

import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.backend.api.CommandEncoderBackend;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanQueryPool;
import com.teslicek.txoptimizations.FramePath;
import com.teslicek.txoptimizations.mixin.gpu.VulkanQueryPoolAccessor;
import org.lwjgl.vulkan.VK12;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;

public final class GpuPassProfiler {

    private static final int                 SLOTS       = 3;
    private static final int                 CAPACITY    = 4096;
    private static final long                DURATION    = 10_000_000_000L;
    private static final int                 CHAT_LINES  = 8;
    private static final String              FRAME_END   = "frame end";
    private static final GpuQueryPool[]      POOLS       = new GpuQueryPool[SLOTS];
    private static final String[][]          LABELS      = new String[SLOTS][CAPACITY];
    private static final int[]               COUNTS      = new int[SLOTS];
    private static final Map<String, Pass>   PASSES      = new HashMap<>();
    private static final Map<Object, String> COPY_LABELS = new IdentityHashMap<>();

    private static VulkanDevice          device;
    private static CommandEncoderBackend encoder;
    private static int                   slot;
    private static boolean               recording;
    private static int                   framesPending;
    private static int                   framesRead;
    private static long                  gpuNanos;
    private static long                  startNanos;
    private static double                nanosPerTick;
    private static long                  terrainDraws;
    private static long                  terrainIndices;

    private GpuPassProfiler() {
    }

    public static void start() {
        if (isRunning())
            throw new IllegalStateException("GPU profile is already running");

        PASSES.clear();
        COPY_LABELS.clear();
        recording      = true;
        framesRead     = 0;
        gpuNanos       = 0L;
        terrainDraws   = 0L;
        terrainIndices = 0L;
        startNanos     = System.nanoTime();
        FramePath.resetCounts();
        CpuSampler.start(Thread.currentThread(), DURATION);
    }

    public static boolean isRunning() {
        return recording || framesPending > 0;
    }

    public static boolean isRecording() {
        return recording;
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

    public static void markPipeline(String pipeline) {
        if (!recording || encoder == null)
            return;

        String label = "pipeline " + pipeline;
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

        double           seconds = (System.nanoTime() - startNanos) / 1.0e9;
        String           gpu     = report(seconds);
        Path             file    = ReportFiles.write("txprofile", gpu + CpuSampler.finish());
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
        report.append(String.format(Locale.ROOT, "Terrain %.0f draws and %.3f million triangles per frame%n", (double) terrainDraws / framesRead, terrainIndices / 3.0 / 1.0e6 / framesRead));

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
