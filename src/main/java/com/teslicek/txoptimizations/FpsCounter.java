package com.teslicek.txoptimizations;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

public final class FpsCounter {

    private static final long WINDOW_NANOS = 500_000_000L;
    private static final int  TEXT_X       = 2;
    private static final int  TEXT_Y       = 2;
    private static final int  TEXT_COLOR   = 0xFFFFFFFF;

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("txoptimizations", "controls"));

    private static KeyMapping            toggleKey;
    private static long                  windowStart = System.nanoTime();
    private static int                   frames;
    private static int                   fps;
    private static FormattedCharSequence text        = label(0);

    private FpsCounter() {
    }

    public static void register() {
        toggleKey = new KeyMapping("key.txoptimizations.fps_counter", InputConstants.KEY_K, CATEGORY);
        KeyMappingHelper.registerKeyMapping(toggleKey);
        ClientTickEvents.END_CLIENT_TICK.register(FpsCounter::onEndClientTick);
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

        if (!TxOptimizationsConfig.fpsCounter() || minecraft.debugEntries.isOverlayVisible() || minecraft.gui.hud.isHidden())
            return;

        graphics.text(minecraft.font, text, TEXT_X, TEXT_Y, TEXT_COLOR, false);
    }

    private static void onEndClientTick(Minecraft minecraft) {
        while (toggleKey.consumeClick()) {
            boolean enabled = !TxOptimizationsConfig.fpsCounter();

            TxOptimizationsConfig.setFpsCounter(enabled);
            minecraft.gui.hud.setOverlayMessage(Component.literal("FPS counter: " + (enabled ? "ON" : "OFF")).withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
        }
    }

    private static FormattedCharSequence label(int value) {
        return FormattedCharSequence.forward(value + " FPS", Style.EMPTY);
    }
}
