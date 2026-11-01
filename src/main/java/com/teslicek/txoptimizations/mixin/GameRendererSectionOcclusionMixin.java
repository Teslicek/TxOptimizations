package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.RenderWorker;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererSectionOcclusionMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    public abstract GameRenderState gameRenderState();

    @Inject(method = "extract", at = @At("TAIL"), require = 1)
    private void txoptimizations$updateSectionOcclusion(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        GameRenderState state = this.gameRenderState();

        if (!state.shouldRenderLevel)
            return;

        LevelRenderer     levelRenderer = this.minecraft.levelRenderer;
        CameraRenderState camera        = state.levelRenderState.cameraRenderState;

        if (!SectionPos.of(camera.pos).equals(levelRenderer.viewArea().getCameraSectionPos()))
            RenderWorker.awaitFromMain();

        ((LevelRendererStateAccessor) levelRenderer).txoptimizations$repositionCamera(camera);
        levelRenderer.sectionOcclusionGraph().update(camera, state.optionsRenderState.fov, state.levelRenderState.chunkLoadingRenderState);
    }
}
