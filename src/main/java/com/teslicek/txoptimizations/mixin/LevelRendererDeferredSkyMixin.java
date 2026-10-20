package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.teslicek.txoptimizations.SkyFarPlane;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererDeferredSkyMixin {

    @Shadow
    @Final
    private GameRenderer gameRenderer;

    @Redirect(method = "lambda$addSkyPass$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;render(Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/state/level/SkyRenderState;)V"))
    private void txoptimizations$deferSky(SkyRenderer renderer, GpuBufferSlice fog, SkyRenderState state) {
        if (!SkyFarPlane.canDefer(state, this.gameRenderer.useImprovedTransparency())) {
            renderer.render(fog, state);

            return;
        }

        SkyFarPlane.defer(renderer, fog, state);
    }

    @Redirect(method = "executeSolid", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeSolid(Lcom/mojang/renderpearl/api/commands/RenderPass;)V"))
    private void txoptimizations$drawSkyAfterTerrain(FeatureRenderDispatcher.PreparedFrame frame, RenderPass pass) {
        if (SkyFarPlane.hasDeferred())
            SkyFarPlane.drawDeferred(pass);

        frame.executeSolid(pass);
    }
}
