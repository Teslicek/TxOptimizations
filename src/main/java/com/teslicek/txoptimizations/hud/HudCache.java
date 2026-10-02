package com.teslicek.txoptimizations.hud;

import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.client.renderer.state.gui.ScreenArea;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

public final class HudCache {

    public static final int PASSES = 3;

    private static final int   MAX_FPS        = 60;
    private static final int   SCREEN_MAX_FPS = 20;
    private static final float POSE_TOLERANCE = 0.01F;
    private static final long  SECOND         = 1_000_000_000L;

    private static final Map<Identifier, HudLayerTimer> LAYERS    = new HashMap<>();
    private static final List<HudLayerTimer>            ORDER     = new ArrayList<>();
    private static final HudLayerTimer                  UNKNOWN   = new HudLayerTimer(false);
    private static final List<ScreenArea>               DEFERRED  = new ArrayList<>();
    private static final Matrix3x2f                     LAST_POSE = new Matrix3x2f();

    private static HudFramebuffers framebuffers;
    private static HudLayerTimer   current;
    private static int             pass = 1;
    private static boolean         rendering;
    private static boolean         flushing;
    private static long            lastFrameNanos;

    private HudCache() {
    }

    public static boolean isRendering() {
        return rendering;
    }

    public static boolean isFlushing() {
        return flushing;
    }

    public static int currentPass() {
        return pass;
    }

    public static HudLayerTimer layer(Identifier id) {
        return LAYERS.getOrDefault(id, UNKNOWN);
    }

    public static void registerLayers(List<Identifier> ids) {
        LAYERS.clear();
        ORDER.clear();

        for (Identifier id : ids) {
            if (LAYERS.containsKey(id))
                continue;

            HudLayerTimer layer = new HudLayerTimer(true);
            LAYERS.put(id, layer);
            ORDER.add(layer);
        }
    }

    public static void begin(HudLayerTimer layer) {
        layer.begin();
        current = layer;
    }

    public static void end(HudLayerTimer layer) {
        current = null;
        layer.end();
    }

    public static boolean isCurrentUncached() {
        return current == null ? !UNKNOWN.isCached() : !current.isCached();
    }

    public static void defer(ScreenArea submission) {
        DEFERRED.add(submission);
    }

    public static void checkBlend(RenderPipeline pipeline) {
        for (ColorTargetState target : pipeline.getColorTargetStates()) {
            if (target == null || target.blendFunction().isEmpty())
                return;

            BlendFunction blend = target.blendFunction().get();

            if (blend.alpha().sourceFactor() != BlendFactor.ONE || blend.alpha().destFactor() != BlendFactor.ONE_MINUS_SRC_ALPHA)
                pipeline.colorTargetStates = withPremultipliedAlpha(pipeline.colorTargetStates, blend);

            if (breaksCaching(blend))
                uncacheCurrent();
        }
    }

    public static void extract(GuiGraphicsExtractor graphics, Runnable hud) {
        HudFramebuffers buffers = framebuffers();

        if (!graphics.pose().equals(LAST_POSE, POSE_TOLERANCE)) {
            LAST_POSE.set(graphics.pose());
            buffers.markForCatchUp();
        }

        if (pass > 0) {
            buffers.resize();
            buffers.bind();
        }

        rendering = true;
        hud.run();

        if (pass > 0)
            flush();

        nextPass(buffers);
        buffers.unbind();
        rendering = false;

        if (buffers.needsCatchUp()) {
            DEFERRED.clear();
            hud.run();

            return;
        }

        replayDeferred(graphics.guiRenderState);
        ScreenRectangle bounds = buffers.frontBounds();

        if (bounds != null)
            graphics.guiRenderState.addGuiElement(new HudBlit(buffers.front(), bounds));
    }

    public static void include(ScreenRectangle bounds) {
        framebuffers().include(bounds);
    }

    public static void markForCatchUp() {
        framebuffers().markForCatchUp();
    }

    public static void reset() {
        lastFrameNanos = System.nanoTime();
        framebuffers().resize();
        framebuffers().markForCatchUp();
    }

    private static void uncacheCurrent() {
        if (current == null)
            throw new IllegalStateException("No HUD layer is being rendered");

        if (!current.isCached())
            return;

        current.uncache();
        framebuffers().dropFrame();
    }

    private static void flush() {
        flushing = true;
        Minecraft.getInstance().gameRenderer.guiRenderer.render();
        flushing = false;
    }

    private static void nextPass(HudFramebuffers buffers) {
        if (pass == 0 && isFrameDue()) {
            pass ++;
            completeCycle(buffers);
        } else if (pass > 0) {
            pass ++;
        }

        if (pass <= PASSES)
            return;

        if (!isFrameDue()) {
            pass = 0;

            return;
        }

        pass = 1;
        completeCycle(buffers);
    }

    private static void completeCycle(HudFramebuffers buffers) {
        distribute();

        if (buffers.swap())
            lastFrameNanos = System.nanoTime();

        HudDeltaTracker.completeCycle();
    }

    private static boolean isFrameDue() {
        int maxFps = Minecraft.getInstance().gui.screen() == null ? MAX_FPS : SCREEN_MAX_FPS;

        return System.nanoTime() >= lastFrameNanos + SECOND / maxFps;
    }

    private static void distribute() {
        long total = 0L;

        for (HudLayerTimer layer : ORDER)
            total += layer.totalTime();

        if (total == 0L) {
            ORDER.forEach(layer -> layer.setPass(1));

            return;
        }

        double target    = (double) total / PASSES;
        double elapsed   = 0.0;
        int    layerPass = 1;

        for (HudLayerTimer layer : ORDER) {
            elapsed += layer.totalTime();

            if (elapsed >= target && layerPass < PASSES) {
                elapsed %= target;
                layerPass ++;
            }

            layer.setPass(layerPass);
        }
    }

    private static void replayDeferred(GuiRenderState state) {
        for (ScreenArea submission : DEFERRED) {
            switch (submission) {
                case GuiElementRenderState element -> state.addGuiElement(element);
                case GuiTextRenderState text -> state.addText(text);
                case PictureInPictureRenderState picture -> state.addPicturesInPictureState(picture);
                case GuiItemRenderState item -> state.addItem(item);
                default -> throw new IllegalStateException("Unknown HUD submission " + submission.getClass().getName());
            }
        }

        DEFERRED.clear();
    }

    private static List<ColorTargetState> withPremultipliedAlpha(List<ColorTargetState> targets, BlendFunction blend) {
        BlendFunction premultiplied = new BlendFunction(blend.color().sourceFactor(), blend.color().destFactor(), BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA);

        return targets.stream().map(target -> new ColorTargetState(Optional.of(premultiplied), target.format(), target.writeMask())).toList();
    }

    private static boolean breaksCaching(BlendFunction blend) {
        BlendFactor source      = blend.color().sourceFactor();
        BlendFactor destination = blend.color().destFactor();

        if (isDestinationColor(source) || isDestinationColor(destination))
            return true;

        if (destination == BlendFactor.SRC_COLOR || destination == BlendFactor.ONE_MINUS_SRC_COLOR)
            return true;

        return source == BlendFactor.ONE && destination == BlendFactor.ONE;
    }

    private static boolean isDestinationColor(BlendFactor factor) {
        return factor == BlendFactor.DST_COLOR || factor == BlendFactor.ONE_MINUS_DST_COLOR;
    }

    private static HudFramebuffers framebuffers() {
        if (framebuffers == null)
            framebuffers = new HudFramebuffers();

        return framebuffers;
    }
}
