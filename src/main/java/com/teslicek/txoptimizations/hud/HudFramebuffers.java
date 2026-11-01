package com.teslicek.txoptimizations.hud;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.renderpearl.api.GpuFormat;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;

final class HudFramebuffers {

    private final HudAreas backAreas = new HudAreas();

    private RenderTarget          back       = new TextureTarget("txoptimizations_hud_back", 1, 1, GpuFormat.RGBA8_UNORM, null);
    private RenderTarget          front      = new TextureTarget("txoptimizations_hud_front", 1, 1, GpuFormat.RGBA8_UNORM, null);
    private List<ScreenRectangle> frontAreas = List.of();
    private ScreenRectangle       frontBounds = ScreenRectangle.empty();
    private boolean               dropFrame;
    private int                   serial;
    private int                   catchUpSerial;
    private int                   width;
    private int                   height;
    private int                   guiScale;

    HudFramebuffers() {
        this.resize();
    }

    void resize() {
        Window window = Minecraft.getInstance().getWindow();

        if (window.getWidth() == this.width && window.getHeight() == this.height && window.getGuiScale() == this.guiScale)
            return;

        this.width      = window.getWidth();
        this.height     = window.getHeight();
        this.guiScale   = window.getGuiScale();
        this.frontAreas  = List.of();
        this.frontBounds = ScreenRectangle.empty();
        this.backAreas.clear();
        this.back.resize(this.width, this.height);
        this.front.resize(this.width, this.height);
        this.markForCatchUp();
    }

    boolean swap() {
        boolean swapped = !this.dropFrame;

        if (swapped) {
            Window       window        = Minecraft.getInstance().getWindow();
            RenderTarget previousFront = this.front;

            this.front      = this.back;
            this.back       = previousFront;
            this.frontAreas  = this.backAreas.rectangles(window.getGuiScaledWidth(), window.getGuiScaledHeight());
            this.frontBounds = this.frontAreas.isEmpty() ? ScreenRectangle.empty() : union(this.frontAreas);
            this.serial ++;
        }

        this.backAreas.clear();

        HudFrameOps.clear(this.back);
        this.dropFrame = false;

        return swapped;
    }

    void dropFrame() {
        this.dropFrame = true;
    }

    void markForCatchUp() {
        this.catchUpSerial = this.serial;
    }

    boolean needsCatchUp() {
        return this.serial - this.catchUpSerial <= 1;
    }

    RenderTarget front() {
        return this.front;
    }

    RenderTarget back() {
        return this.back;
    }

    List<ScreenRectangle> frontAreas() {
        return this.frontAreas;
    }

    ScreenRectangle frontBounds() {
        return this.frontBounds;
    }

    void include(ScreenRectangle bounds) {
        if (bounds == null)
            return;

        this.backAreas.include(bounds.left(), bounds.top(), bounds.right(), bounds.bottom());
    }

    private static ScreenRectangle union(List<ScreenRectangle> areas) {
        int left   = Integer.MAX_VALUE;
        int top    = Integer.MAX_VALUE;
        int right  = Integer.MIN_VALUE;
        int bottom = Integer.MIN_VALUE;

        for (ScreenRectangle area : areas) {
            left   = Math.min(left, area.left());
            top    = Math.min(top, area.top());
            right  = Math.max(right, area.right());
            bottom = Math.max(bottom, area.bottom());
        }

        return new ScreenRectangle(left, top, right - left, bottom - top);
    }
}
