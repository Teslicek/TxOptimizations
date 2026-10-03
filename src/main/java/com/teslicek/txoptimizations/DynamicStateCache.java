package com.teslicek.txoptimizations;

public final class DynamicStateCache {

    private static long    commandBuffer;
    private static boolean viewportSet;
    private static float   viewportWidth;
    private static float   viewportHeight;
    private static boolean scissorSet;
    private static int     scissorX;
    private static int     scissorY;
    private static int     scissorWidth;
    private static int     scissorHeight;

    private DynamicStateCache() {
    }

    public static void reset() {
        commandBuffer = 0L;
        viewportSet   = false;
        scissorSet    = false;
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
    }
}
