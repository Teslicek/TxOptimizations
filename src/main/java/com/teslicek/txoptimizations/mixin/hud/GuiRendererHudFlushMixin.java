package com.teslicek.txoptimizations.mixin.hud;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.teslicek.txoptimizations.hud.HudCache;
import com.teslicek.txoptimizations.hud.HudCacheRenderer;
import com.teslicek.txoptimizations.hud.HudShaderOverrides;
import com.teslicek.txoptimizations.mixin.GuiRendererStateAccessor;
import java.util.List;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererHudFlushMixin implements HudCacheRenderer {

    @Shadow
    @Final
    private GuiRenderState renderState;

    @Shadow
    @Final
    private List<?> draws;

    @Shadow
    @Final
    private StagedVertexBuffer vertexBuffer;

    @Shadow
    @Final
    private CubeMap cubeMap;

    @Shadow
    private int firstDrawIndexAfterBlur;

    @Shadow
    private void prepare() {
        throw new AssertionError();
    }

    @Shadow
    private void draw() {
        throw new AssertionError();
    }

    @Shadow
    private void clearUnusedOversizedItemRenderers() {
        throw new AssertionError();
    }

    @ModifyExpressionValue(method = "executeDraw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer$Draw;pipeline()Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;"))
    private RenderPipeline txoptimizations$checkMovedHud(RenderPipeline pipeline) {
        if (HudCache.isFlushing())
            HudShaderOverrides.check(pipeline);

        return pipeline;
    }

    @Override
    public void txoptimizations$renderHudCache(GuiRenderState state) {
        ((GuiRendererStateAccessor) this).txoptimizations$setRenderState(state);

        ProfilerFiller profiler = Profiler.get();

        if (this.renderState.panoramaRenderState != null)
            this.cubeMap.render(10.0F, this.renderState.panoramaRenderState.spin());

        profiler.push("prepare");
        this.prepare();
        profiler.popPush("upload");
        this.vertexBuffer.upload();
        RenderSystem.resizeAllAutoStorageIndexBuffers();
        profiler.popPush("draw");
        this.draw();
        profiler.popPush("endFrame");
        this.vertexBuffer.endDraw();
        this.vertexBuffer.endFrame();
        this.draws.clear();
        this.renderState.reset();
        this.firstDrawIndexAfterBlur = Integer.MAX_VALUE;
        this.clearUnusedOversizedItemRenderers();
        profiler.pop();
    }
}
