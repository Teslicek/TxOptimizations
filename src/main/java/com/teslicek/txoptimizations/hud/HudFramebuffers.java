package com.teslicek.txoptimizations.hud;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Vector4f;
import org.joml.Vector4fc;

final class HudFramebuffers {

    private static final Vector4fc CLEAR_COLOR    = new Vector4f(0.0F);
    private static final int       BOUNDS_PADDING = 2;

    private RenderTarget    back      = new TextureTarget("txoptimizations_hud_back", 1, 1, GpuFormat.RGBA8_UNORM, null);
    private RenderTarget    front     = new TextureTarget("txoptimizations_hud_front", 1, 1, GpuFormat.RGBA8_UNORM, null);
    private boolean         backEmpty = true;
    private RenderTarget    mainTarget;
    private boolean         dropFrame;
    private int             serial;
    private int             catchUpSerial;
    private int             width;
    private int             height;
    private int             guiScale;
    private int             backLeft;
    private int             backTop;
    private int             backRight;
    private int             backBottom;
    private ScreenRectangle frontBounds;

    HudFramebuffers() {
        this.resize();
    }

    void resize() {
        Window window = Minecraft.getInstance().getWindow();

        if (window.getWidth() == this.width && window.getHeight() == this.height && window.getGuiScale() == this.guiScale)
            return;

        this.width       = window.getWidth();
        this.height      = window.getHeight();
        this.guiScale    = window.getGuiScale();
        this.frontBounds = null;
        this.backEmpty   = true;
        this.back.resize(this.width, this.height);
        this.front.resize(this.width, this.height);
        this.markForCatchUp();
    }

    boolean swap() {
        boolean swapped = !this.dropFrame;

        if (swapped) {
            RenderTarget previousFront = this.front;
            this.front       = this.back;
            this.back        = previousFront;
            this.frontBounds = this.backBounds();
            this.serial ++;
        }

        this.backEmpty = true;

        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.back.getColorTexture(), CLEAR_COLOR);
        this.dropFrame = false;

        return swapped;
    }

    void dropFrame() {
        this.dropFrame = true;
    }

    void bind() {
        GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;

        if (this.mainTarget != null)
            throw new IllegalStateException("HUD framebuffer is already bound");

        this.mainTarget               = gameRenderer.mainRenderTarget;
        gameRenderer.mainRenderTarget = this.back;
    }

    void unbind() {
        if (this.mainTarget == null)
            return;

        Minecraft.getInstance().gameRenderer.mainRenderTarget = this.mainTarget;
        this.mainTarget                                       = null;
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

    ScreenRectangle frontBounds() {
        return this.frontBounds;
    }

    void include(ScreenRectangle bounds) {
        Window window = Minecraft.getInstance().getWindow();
        int    left   = bounds == null ? 0 : bounds.left();
        int    top    = bounds == null ? 0 : bounds.top();
        int    right  = bounds == null ? window.getGuiScaledWidth() : bounds.right();
        int    bottom = bounds == null ? window.getGuiScaledHeight() : bounds.bottom();

        if (this.backEmpty) {
            this.backLeft   = left;
            this.backTop    = top;
            this.backRight  = right;
            this.backBottom = bottom;
            this.backEmpty  = false;

            return;
        }

        this.backLeft   = Math.min(this.backLeft, left);
        this.backTop    = Math.min(this.backTop, top);
        this.backRight  = Math.max(this.backRight, right);
        this.backBottom = Math.max(this.backBottom, bottom);
    }

    private ScreenRectangle backBounds() {
        if (this.backEmpty)
            return null;

        Window window = Minecraft.getInstance().getWindow();
        int    left   = Math.max(this.backLeft - BOUNDS_PADDING, 0);
        int    top    = Math.max(this.backTop - BOUNDS_PADDING, 0);
        int    right  = Math.min(this.backRight + BOUNDS_PADDING, window.getGuiScaledWidth());
        int    bottom = Math.min(this.backBottom + BOUNDS_PADDING, window.getGuiScaledHeight());

        if (right <= left || bottom <= top)
            return null;

        return new ScreenRectangle(left, top, right - left, bottom - top);
    }
}
