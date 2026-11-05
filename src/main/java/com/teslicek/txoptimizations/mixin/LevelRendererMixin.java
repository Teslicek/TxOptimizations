package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.teslicek.txoptimizations.BackgroundVertexCopy;
import com.teslicek.txoptimizations.ClientClock;
import com.teslicek.txoptimizations.CompactTerrain;
import com.teslicek.txoptimizations.DisplayCulling;
import com.teslicek.txoptimizations.OutlineScissor;
import com.teslicek.txoptimizations.PackedGizmoPrimitives;
import com.teslicek.txoptimizations.PackedTerrain;
import com.teslicek.txoptimizations.ParallelFeatures;
import com.teslicek.txoptimizations.RenderStateBuffers;
import com.teslicek.txoptimizations.RenderWorker;
import com.teslicek.txoptimizations.SkyFarPlane;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Options;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ChunkLoadingRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Redirect(method = "isSectionCompiledAndVisible", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getMillis()J"))
    private long txoptimizations$frameMillis() {
        return ClientClock.frameMillis();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void txoptimizations$beginOutlineFrame(CallbackInfo ci) {
        OutlineScissor.beginFrame();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ShaderManager;getPostChain(Lnet/minecraft/resources/Identifier;Ljava/util/Set;)Lnet/minecraft/client/renderer/PostChain;"))
    private PostChain txoptimizations$trackOutlineChain(ShaderManager shaderManager, Identifier id, Set<Identifier> targets) {
        PostChain chain = shaderManager.getPostChain(id, targets);

        OutlineScissor.useChain(chain);

        return chain;
    }

    @Redirect(method = "blitEntityOutline", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitAndBlendToTexture(Lcom/mojang/renderpearl/api/textures/GpuTextureView;Lcom/mojang/renderpearl/api/textures/GpuTextureView;)V"))
    private void txoptimizations$blendInsideOutline(RenderTarget target, GpuTextureView color, GpuTextureView depth) {
        OutlineScissor.beginBlit();
        target.blitAndBlendToTexture(color, depth);
        OutlineScissor.endBlit();
    }

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

    @Unique
    private final DrawableGizmoPrimitives[] txoptimizations$standard = new DrawableGizmoPrimitives[2];

    @Unique
    private final DrawableGizmoPrimitives[] txoptimizations$alwaysOnTop = new DrawableGizmoPrimitives[2];

    @Redirect(method = "finalizeGizmoCollection", at = @At(value = "NEW", target = "()Lnet/minecraft/client/renderer/gizmos/DrawableGizmoPrimitives;", ordinal = 0))
    private DrawableGizmoPrimitives txoptimizations$reuseStandard() {
        return txoptimizations$reuse(this.txoptimizations$standard);
    }

    @Redirect(method = "finalizeGizmoCollection", at = @At(value = "NEW", target = "()Lnet/minecraft/client/renderer/gizmos/DrawableGizmoPrimitives;", ordinal = 1))
    private DrawableGizmoPrimitives txoptimizations$reuseAlwaysOnTop() {
        return txoptimizations$reuse(this.txoptimizations$alwaysOnTop);
    }

    @Unique
    private static DrawableGizmoPrimitives txoptimizations$reuse(DrawableGizmoPrimitives[] perFrame) {
        int parity = RenderStateBuffers.parity();

        if (perFrame[parity] == null)
            perFrame[parity] = new DrawableGizmoPrimitives();
        else
            ((PackedGizmoPrimitives) perFrame[parity]).txoptimizations$clear();

        return perFrame[parity];
    }

    @Shadow
    private @Nullable SectionRenderDispatcher sectionRenderDispatcher;

    @Shadow
    public abstract void resetLevelRenderData();

    @Inject(method = "invalidateCompiledGeometry", at = @At("HEAD"), require = 1)
    private void txoptimizations$matchTerrainFormat(ClientLevel level, Options options, Camera camera, BlockColors blockColors, CallbackInfo ci) {
        if (this.sectionRenderDispatcher != null && CompactTerrain.active() != PackedTerrain.isPackedSource())
            this.resetLevelRenderData();
    }

    @Inject(method = "submitFeatures", at = @At("HEAD"))
    private void txoptimizations$beginDisplayCulling(LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector, boolean renderOutline, CallbackInfo ci) {
        DisplayCulling.begin(levelRenderState.cameraRenderState.pos);
    }

    @Inject(method = "submitFeatures", at = @At("RETURN"))
    private void txoptimizations$endDisplayCulling(LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector, boolean renderOutline, CallbackInfo ci) {
        DisplayCulling.end();
    }

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;prepareFrame(Lnet/minecraft/client/renderer/SubmitNodeStorage;)Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;"))
    private void txoptimizations$allowParallelFeatures(CallbackInfo ci) {
        ParallelFeatures.allowNextFrame(!((BackgroundVertexCopy) this.renderBuffers.stagedVertexBuffer()).txoptimizations$isCopyingInBackground());
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;prepareFrame(Lnet/minecraft/client/renderer/SubmitNodeStorage;)Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;", shift = At.Shift.AFTER))
    private void txoptimizations$copyFeatureVerticesInBackground(CallbackInfo ci) {
        ((BackgroundVertexCopy) this.renderBuffers.stagedVertexBuffer()).txoptimizations$copyVerticesInBackground();
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SectionOcclusionGraph;update(Lnet/minecraft/client/renderer/state/level/CameraRenderState;ILnet/minecraft/client/renderer/state/level/ChunkLoadingRenderState;)V"), require = 1)
    private void txoptimizations$updatedDuringExtraction(SectionOcclusionGraph graph, CameraRenderState camera, int fov, ChunkLoadingRenderState chunkLoading, Operation<Void> original) {
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;repositionCamera(Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"), require = 1)
    private void txoptimizations$repositionedDuringExtraction(LevelRenderer renderer, CameraRenderState camera, Operation<Void> original) {
    }

    @Inject(method = {"invalidateCompiledGeometry", "resetLevelRenderData"}, at = @At("HEAD"), require = 2)
    private void txoptimizations$waitBeforeInvalidating(CallbackInfo ci) {
        RenderWorker.awaitFromMain();
    }

    @Redirect(method = {"render", "submitFeatures"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;submitNodeStorage:Lnet/minecraft/client/renderer/SubmitNodeStorage;", opcode = Opcodes.GETFIELD), require = 5)
    private SubmitNodeStorage txoptimizations$storageForRole(LevelRenderer renderer) {
        return RenderStateBuffers.storageForRole();
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;submitFeatures(Lnet/minecraft/client/renderer/state/level/LevelRenderState;Lnet/minecraft/client/renderer/SubmitNodeCollector;Z)V"), require = 1)
    private void txoptimizations$submittedDuringExtraction(LevelRenderer renderer, LevelRenderState levelRenderState, SubmitNodeCollector collector, boolean renderBlockOutline, Operation<Void> original) {
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeStorage;setUseImprovedTransparency(Z)V"), require = 1)
    private void txoptimizations$transparencySetDuringExtraction(SubmitNodeStorage storage, boolean useImprovedTransparency, Operation<Void> original) {
    }

    @Inject(method = "frameHasAlwaysOnTopGizmos", at = @At("HEAD"), cancellable = true, require = 1)
    private void txoptimizations$renderFrameAlwaysOnTopGizmos(CallbackInfoReturnable<Boolean> cir) {
        if (RenderWorker.isCurrent())
            cir.setReturnValue(RenderStateBuffers.renderAlwaysOnTopGizmos());
    }
}
