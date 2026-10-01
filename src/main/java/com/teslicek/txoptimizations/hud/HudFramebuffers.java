package com.teslicek.txoptimizations.hud;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Vector4f;
import org.joml.Vector4fc;

final class HudFramebuffers {

    private static final Vector4fc CLEAR_COLOR = new Vector4f(0.0F);

    private RenderTarget back  = new TextureTarget("txoptimizations_hud_back", 1, 1, GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
    private RenderTarget front = new TextureTarget("txoptimizations_hud_front", 1, 1, GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
    private RenderTarget mainTarget;
    private boolean      dropFrame;
    private int          serial;
    private int          catchUpSerial;
    private int          width;
    private int          height;
    private int          guiScale;

    HudFramebuffers() {
        this.resize();
    }

    void resize() {
        Window window = Minecraft.getInstance().getWindow();

        if (window.getWidth() == this.width && window.getHeight() == this.height && window.getGuiScale() == this.guiScale)
            return;

        this.width    = window.getWidth();
        this.height   = window.getHeight();
        this.guiScale = window.getGuiScale();
        this.back.resize(this.width, this.height);
        this.front.resize(this.width, this.height);
        this.markForCatchUp();
    }

    boolean swap() {
        boolean swapped = !this.dropFrame;

        if (swapped) {
            RenderTarget previousFront = this.front;
            this.front = this.back;
            this.back  = previousFront;
            this.serial ++;
        }

        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(this.back.getColorTexture(), CLEAR_COLOR, this.back.getDepthTexture(), 1.0);
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
}
