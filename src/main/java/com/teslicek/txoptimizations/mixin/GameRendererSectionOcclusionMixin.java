package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.RenderStateBuffers;
import com.teslicek.txoptimizations.RenderWorker;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.Gizmos;
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

    @Shadow
    public abstract boolean useImprovedTransparency();

    @Shadow
    private boolean shouldRenderBlockOutline() {
        throw new AssertionError();
    }

    @Inject(method = "extract", at = @At("TAIL"), require = 1)
    private void txoptimizations$updateSectionOcclusion(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        GameRenderState state = this.gameRenderState();

        if (!state.shouldRenderLevel)
            return;

        LevelRenderer     levelRenderer = this.minecraft.levelRenderer;
        CameraRenderState camera        = state.levelRenderState.cameraRenderState;

        if (!SectionPos.of(camera.pos).equals(levelRenderer.viewArea().getCameraSectionPos()))
            RenderWorker.awaitFromMain();

        LevelRendererStateAccessor accessor = (LevelRendererStateAccessor) levelRenderer;
        SubmitNodeStorage          storage  = RenderStateBuffers.storageForRole();

        accessor.txoptimizations$repositionCamera(camera);
        levelRenderer.sectionOcclusionGraph().update(camera, state.optionsRenderState.fov, state.levelRenderState.chunkLoadingRenderState);
        storage.setUseImprovedTransparency(this.useImprovedTransparency());

        try (Gizmos.TemporaryCollection ignored = levelRenderer.collectPerFrameRenderThreadGizmos()) {
            accessor.txoptimizations$submitFeatures(state.levelRenderState, storage, this.shouldRenderBlockOutline());
        }

        RenderStateBuffers.setAlwaysOnTopGizmos(accessor.txoptimizations$frameHasAlwaysOnTopGizmos());
    }
}
