package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.teslicek.txoptimizations.SkyFarPlane;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererDeferredSkyMixin {

    @Shadow
    @Final
    private GameRenderer gameRenderer;

    @Unique
    private RenderPass txoptimizations$afterSkyPass;

    @WrapOperation(method = "lambda$addSkyPass$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;render(Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/state/level/SkyRenderState;)V"))
    private void txoptimizations$deferSky(SkyRenderer renderer, GpuBufferSlice fog, SkyRenderState state, Operation<Void> original) {
        if (!SkyFarPlane.canDefer(state, this.gameRenderer.useImprovedTransparency())) {
            original.call(renderer, fog, state);

            return;
        }

        SkyFarPlane.defer(renderer, fog, state);
    }

    @WrapOperation(method = "executeSolid", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeSolid(Lcom/mojang/renderpearl/api/commands/RenderPass;)V"))
    private void txoptimizations$drawSkyAfterTerrain(FeatureRenderDispatcher.PreparedFrame frame, RenderPass pass, Operation<Void> original) {
        if (!SkyFarPlane.hasDeferred()) {
            original.call(frame, pass);

            return;
        }

        if (this.txoptimizations$afterSkyPass != null)
            throw new IllegalStateException("The previous frame's pass after the sky was never closed");

        RenderTarget mainTarget = this.gameRenderer.mainRenderTarget();

        pass.close();
        SkyFarPlane.drawDeferred();

        RenderPass afterSky = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Main", mainTarget.getColorTextureView(), Optional.empty(), mainTarget.getDepthTextureView(), OptionalDouble.empty());

        RenderSystem.bindDefaultUniforms(afterSky);
        this.txoptimizations$afterSkyPass = afterSky;
        original.call(frame, afterSky);
    }

    @WrapOperation(method = "lambda$addMainPass$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;executeClassicTransparency(Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;Lcom/mojang/renderpearl/api/commands/RenderPass;)V"))
    private void txoptimizations$continueAfterSky(LevelRenderer renderer, ChunkSectionsToRender sections, FeatureRenderDispatcher.PreparedFrame frame, RenderPass pass, Operation<Void> original) {
        RenderPass afterSky = this.txoptimizations$afterSkyPass;

        if (afterSky == null) {
            original.call(renderer, sections, frame, pass);

            return;
        }

        this.txoptimizations$afterSkyPass = null;

        try (afterSky) {
            original.call(renderer, sections, frame, afterSky);
        }
    }
}
