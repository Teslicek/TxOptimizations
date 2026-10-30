package com.teslicek.txoptimizations;

import it.unimi.dsi.fastutil.objects.ReferenceList;
import java.util.Arrays;
import java.util.Objects;

public final class DynamicStateCache {

    public static final long ALL_DESCRIPTORS = -1L;

    private static long     commandBuffer;
    private static boolean  viewportSet;
    private static float    viewportWidth;
    private static float    viewportHeight;
    private static boolean  scissorSet;
    private static int      scissorX;
    private static int      scissorY;
    private static int      scissorWidth;
    private static int      scissorHeight;
    private static long     pipeline;
    private static long     descriptorLayout;
    private static int      descriptorCount;
    private static Object[] descriptors = new Object[16];
    private static long     pushedLayout;

    private DynamicStateCache() {
    }

    public static void reset() {
        commandBuffer = 0L;
        viewportSet   = false;
        scissorSet    = false;
        pipeline      = 0L;
        forgetDescriptors();
    }

    public static boolean changesPipeline(long buffer, long boundPipeline) {
        if (buffer == commandBuffer && boundPipeline == pipeline)
            return false;

        track(buffer);
        pipeline = boundPipeline;

        return true;
    }

    public static long changedDescriptors(long buffer, long layout, long pipelineLayout, ReferenceList<Object> values) {
        int     count   = values.size();
        boolean known   = buffer == commandBuffer && layout == descriptorLayout && count == descriptorCount;
        long    changed = known ? differingDescriptors(values, count) : ALL_DESCRIPTORS;

        if (changed == 0L)
            return 0L;

        if (pipelineLayout != pushedLayout || count > Long.SIZE)
            changed = ALL_DESCRIPTORS;

        track(buffer);
        pushedLayout = pipelineLayout;

        if (descriptors.length < count)
            descriptors = new Object[count];

        values.getElements(0, descriptors, 0, count);
        descriptorLayout = layout;
        descriptorCount  = count;

        return changed;
    }

    public static boolean changesViewport(long buffer, float width, float height) {
        if (buffer == commandBuffer && viewportSet && width == viewportWidth && height == viewportHeight)
            return false;

        track(buffer);
        viewportSet    = true;
        viewportWidth  = width;
        viewportHeight = height;

        return true;
    }

    public static boolean changesScissor(long buffer, int x, int y, int width, int height) {
        if (buffer == commandBuffer && scissorSet && x == scissorX && y == scissorY && width == scissorWidth && height == scissorHeight)
            return false;

        track(buffer);
        scissorSet    = true;
        scissorX      = x;
        scissorY      = y;
        scissorWidth  = width;
        scissorHeight = height;

        return true;
    }

    private static void track(long buffer) {
        if (buffer == commandBuffer)
            return;

        commandBuffer = buffer;
        viewportSet   = false;
        scissorSet    = false;
        pipeline      = 0L;
        forgetDescriptors();
    }

    private static long differingDescriptors(ReferenceList<Object> values, int count) {
        long differing = 0L;

        for (int index = 0; index < Math.min(count, Long.SIZE); index ++) {
            if (!Objects.equals(descriptors[index], values.get(index)))
                differing |= 1L << index;
        }

        for (int index = Long.SIZE; index < count; index ++) {
            if (!Objects.equals(descriptors[index], values.get(index)))
                return ALL_DESCRIPTORS;
        }

        return differing;
    }

    private static void forgetDescriptors() {
        Arrays.fill(descriptors, 0, descriptorCount, null);
        descriptorLayout = 0L;
        descriptorCount  = 0;
        pushedLayout     = 0L;
    }
}
