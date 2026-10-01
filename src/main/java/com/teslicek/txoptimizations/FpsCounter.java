package com.teslicek.txoptimizations;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public final class FpsCounter {

    private static final long WINDOW_NANOS = 500_000_000L;
    private static final int  TEXT_X       = 2;
    private static final int  TEXT_Y       = 2;
    private static final int  TEXT_COLOR   = 0xFFFFFFFF;

    private static long                  windowStart = System.nanoTime();
    private static int                   frames;
    private static int                   fps;
    private static FormattedCharSequence text        = label(0);

    private FpsCounter() {
    }

    public static void countFrame() {
        long now     = System.nanoTime();
        long elapsed = now - windowStart;

        frames ++;

        if (elapsed < WINDOW_NANOS)
            return;

        int measured = (int) Math.round(frames * 1_000_000_000.0 / elapsed);
        windowStart  = now;
        frames       = 0;

        if (measured == fps)
            return;

        fps  = measured;
        text = label(measured);
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.debugEntries.isOverlayVisible() || minecraft.gui.hud.isHidden())
            return;

        graphics.text(minecraft.font, text, TEXT_X, TEXT_Y, TEXT_COLOR, false);
    }

    private static FormattedCharSequence label(int value) {
        return FormattedCharSequence.forward(value + " FPS", Style.EMPTY);
    }
}
