package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
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

        this.minecraft.levelRenderer.sectionOcclusionGraph().update(state.levelRenderState.cameraRenderState, state.optionsRenderState.fov, state.levelRenderState.chunkLoadingRenderState);
    }
}
