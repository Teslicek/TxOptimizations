package com.teslicek.txoptimizations.gpu;

import java.nio.LongBuffer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueryPoolCreateInfo;

public final class FragmentCounts {

    private static final int                  SLOTS   = 8192;
    private static final ArrayDeque<Pending>  PENDING = new ArrayDeque<>();
    private static final Map<String, long[]>  TOTALS  = new HashMap<>();

    private static VkDevice        device;
    private static long            pool;
    private static int             nextSlot;
    private static VkCommandBuffer openBuffer;
    private static String          openLabel;
    private static int             openSlot;

    private FragmentCounts() {
    }

    static void reset() {
        TOTALS.clear();
    }

    public static void begin(VkDevice vkDevice, VkCommandBuffer commandBuffer, String label) {
        if (openLabel != null && openLabel.equals(label) && openBuffer == commandBuffer)
            return;

        end();

        if (pool == 0L)
            createPool(vkDevice);

        int slot = nextSlot;

        nextSlot = (nextSlot + 1) % SLOTS;
        VK12.vkResetQueryPool(device, pool, slot, 1);
        VK10.vkCmdBeginQuery(commandBuffer, pool, slot, 0);
        openBuffer = commandBuffer;
        openLabel  = label;
        openSlot   = slot;
    }

    public static void end() {
        if (openLabel == null)
            return;

        VK10.vkCmdEndQuery(openBuffer, pool, openSlot);
        PENDING.addLast(new Pending(openLabel, openSlot));
        openBuffer = null;
        openLabel  = null;
    }

    static void drain() {
        if (pool == 0L)
            return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer values = stack.mallocLong(2);

            while (!PENDING.isEmpty()) {
                Pending query  = PENDING.peekFirst();
                int     result = VK10.vkGetQueryPoolResults(device, pool, query.slot(), 1, values, 2L * Long.BYTES, VK10.VK_QUERY_RESULT_64_BIT | VK10.VK_QUERY_RESULT_WITH_AVAILABILITY_BIT);

                if (result != VK10.VK_SUCCESS && result != VK10.VK_NOT_READY)
                    throw new IllegalStateException("Reading fragment statistics failed: " + result);

                if (values.get(1) == 0L)
                    return;

                PENDING.removeFirst();
                TOTALS.computeIfAbsent(query.label(), label -> new long[1])[0] += values.get(0);
            }
        }
    }

    static String report(int frames) {
        StringBuilder report = new StringBuilder("Fragments shaded per frame by pipeline:");

        TOTALS.entrySet().stream()
            .filter(entry -> entry.getValue()[0] > 0L)
            .sorted((first, second) -> Long.compare(second.getValue()[0], first.getValue()[0]))
            .forEach(entry -> report.append(String.format(Locale.ROOT, "%n  %10.0f  %s", (double) entry.getValue()[0] / frames, entry.getKey())));

        return report.append(String.format(Locale.ROOT, "%n%n")).toString();
    }

    private static void createPool(VkDevice vkDevice) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkQueryPoolCreateInfo info   = VkQueryPoolCreateInfo.calloc(stack).sType$Default().queryType(VK10.VK_QUERY_TYPE_PIPELINE_STATISTICS).queryCount(SLOTS).pipelineStatistics(VK10.VK_QUERY_PIPELINE_STATISTIC_FRAGMENT_SHADER_INVOCATIONS_BIT);
            LongBuffer            handle = stack.mallocLong(1);
            int                   result = VK10.vkCreateQueryPool(vkDevice, info, null, handle);

            if (result != VK10.VK_SUCCESS)
                throw new IllegalStateException("Cannot create the fragment statistics query pool: " + result);

            device = vkDevice;
            pool   = handle.get(0);
            VK12.vkResetQueryPool(device, pool, 0, SLOTS);
        }
    }

    private record Pending(String label, int slot) {
    }
}
