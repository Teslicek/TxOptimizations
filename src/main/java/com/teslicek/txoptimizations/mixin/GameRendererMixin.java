package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.teslicek.txoptimizations.EmptyHashSet;
import com.teslicek.txoptimizations.LevelProjection;
import com.teslicek.txoptimizations.PersistentRingBuffer;
import com.teslicek.txoptimizations.RenderStateBuffers;
import com.teslicek.txoptimizations.RenderWorker;
import java.util.Collection;
import java.util.HashSet;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.Gizmos;
import org.joml.Matrix4f;
import org.joml.Vector4fc;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Unique
    private final MappableRingBuffer txoptimizations$levelProjection = new MappableRingBuffer(() -> "Level Projection UBO", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);

    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"))
    private GpuBufferSlice txoptimizations$writeLevelProjection(ProjectionMatrixBuffer buffer, Matrix4f projectionMatrix) {
        this.txoptimizations$levelProjection.rotate();

        GpuBuffer target = this.txoptimizations$levelProjection.currentBuffer();

        Std140Builder.intoBuffer(((PersistentRingBuffer) this.txoptimizations$levelProjection).txoptimizations$mapCurrent().clear()).putMat4f(projectionMatrix);

        GpuBufferSlice slice = target.slice(0L, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);

        LevelProjection.capture(projectionMatrix, slice);

        return slice;
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void txoptimizations$closeLevelProjection(CallbackInfo ci) {
        this.txoptimizations$levelProjection.close();
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/CommandEncoder;clearColorAndDepthTextures(Lcom/mojang/renderpearl/api/textures/GpuTexture;Lorg/joml/Vector4fc;Lcom/mojang/renderpearl/api/textures/GpuTexture;D)V"))
    private void txoptimizations$skipClearBeforeLevel(CommandEncoder encoder, GpuTexture colorTexture, Vector4fc clearColor, GpuTexture depthTexture, double clearDepth, Operation<Void> original) {
        if (RenderStateBuffers.renderTree().shouldRenderLevel)
            return;

        original.call(encoder, colorTexture, clearColor, depthTexture, clearDepth);
    }

    @Redirect(method = "preparePostEffects", at = @At(value = "NEW", target = "(Ljava/util/Collection;)Ljava/util/HashSet;"), require = 1)
    private HashSet<Object> txoptimizations$previousEffects(Collection<Object> applied) {
        if (applied.isEmpty())
            return EmptyHashSet.instance();

        return new HashSet<>(applied);
    }

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

    @Redirect(method = {"render", "renderLevel", "render3dHud", "renderItemInHand"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;gameRenderState:Lnet/minecraft/client/renderer/state/GameRenderState;", opcode = Opcodes.GETFIELD), require = 4)
    private GameRenderState txoptimizations$renderTree(GameRenderer renderer) {
        return RenderStateBuffers.renderTree();
    }

    @Redirect(method = {"gameRenderState", "useImprovedTransparency", "bobHurt"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;gameRenderState:Lnet/minecraft/client/renderer/state/GameRenderState;", opcode = Opcodes.GETFIELD), require = 3)
    private GameRenderState txoptimizations$treeForRole(GameRenderer renderer) {
        return RenderStateBuffers.forRole(((GameRendererStateAccessor) renderer).txoptimizations$gameRenderState());
    }
}
