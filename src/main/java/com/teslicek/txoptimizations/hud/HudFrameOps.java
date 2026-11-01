package com.teslicek.txoptimizations.hud;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teslicek.txoptimizations.RenderWorker;
import com.teslicek.txoptimizations.mixin.GuiRenderStateMoveAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.joml.Vector4f;
import org.joml.Vector4fc;

public final class HudFrameOps {

    private static final Vector4fc                             CLEAR_COLOR = new Vector4f(0.0F);
    private static final ConcurrentLinkedQueue<GuiRenderState> FREE        = new ConcurrentLinkedQueue<>();

    private static List<Op> pending = new ArrayList<>();

    private HudFrameOps() {
    }

    static void flush(GuiRenderState extracted, RenderTarget target) {
        requireExtraction();

        GuiRenderState state = FREE.poll();

        if (state == null)
            state = new GuiRenderState();

        move(extracted, state);
        pending.add(new Flush(state, target));
    }

    static void clear(RenderTarget target) {
        requireExtraction();
        pending.add(new Clear(target));
    }

    public static List<Op> takeForFrame() {
        requireExtraction();

        if (pending.isEmpty())
            return List.of();

        List<Op> frame = pending;

        pending = new ArrayList<>();

        return frame;
    }

    public static void run(List<Op> ops) {
        if (ops.isEmpty())
            return;

        if (!RenderWorker.isCurrent())
            throw new IllegalStateException("HUD cache operations ran outside the render worker");

        for (Op op : ops) {
            switch (op) {
                case Flush flush -> runFlush(flush);
                case Clear clear -> RenderSystem.getDevice().createCommandEncoder().clearColorTexture(clear.target.getColorTexture(), CLEAR_COLOR);
            }
        }
    }

    private static void runFlush(Flush flush) {
        GameRenderer renderer = Minecraft.getInstance().gameRenderer;
        RenderTarget main     = renderer.mainRenderTarget;

        renderer.mainRenderTarget = flush.target;
        HudCache.setFlushing(true);

        try {
            ((HudCacheRenderer) renderer.guiRenderer).txoptimizations$renderHudCache(flush.state);
        } finally {
            HudCache.setFlushing(false);
            renderer.mainRenderTarget = main;
        }

        FREE.add(flush.state);
    }

    private static void move(GuiRenderState from, GuiRenderState to) {
        GuiRenderStateMoveAccessor source = (GuiRenderStateMoveAccessor) from;
        GuiRenderStateMoveAccessor target = (GuiRenderStateMoveAccessor) to;

        target.txoptimizations$strata().clear();
        target.txoptimizations$strata().addAll(source.txoptimizations$strata());
        target.txoptimizations$itemModelIdentities().clear();
        target.txoptimizations$itemModelIdentities().addAll(source.txoptimizations$itemModelIdentities());
        target.txoptimizations$setCurrent(source.txoptimizations$current());
        target.txoptimizations$setFirstStratumAfterBlur(source.txoptimizations$firstStratumAfterBlur());
        target.txoptimizations$setWindowRectangleForDebug(source.txoptimizations$windowRectangleForDebug());
        to.panoramaRenderState = from.panoramaRenderState;
        to.clearColorOverride.set(from.clearColorOverride);
        to.isHudHidden = from.isHudHidden;
        from.reset();
    }

    private static void requireExtraction() {
        if (RenderWorker.isCurrent())
            throw new IllegalStateException("HUD cache operations were recorded on the render worker");
    }

    public sealed interface Op permits Flush, Clear {
    }

    private record Flush(GuiRenderState state, RenderTarget target) implements Op {
    }

    private record Clear(RenderTarget target) implements Op {
    }
}
