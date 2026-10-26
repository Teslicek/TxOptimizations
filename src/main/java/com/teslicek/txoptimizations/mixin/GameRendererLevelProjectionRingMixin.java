package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuBuffer;
import com.teslicek.txoptimizations.LevelProjection;
import com.teslicek.txoptimizations.PersistentRingBuffer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererLevelProjectionRingMixin {

    @Unique
    private final MappableRingBuffer txoptimizations$levelProjection = new MappableRingBuffer(() -> "Level Projection UBO", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);

    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"))
    private GpuBufferSlice txoptimizations$writeLevelProjection(ProjectionMatrixBuffer buffer, Matrix4f projectionMatrix) {
        this.txoptimizations$levelProjection.rotate();

        GpuBuffer target = this.txoptimizations$levelProjection.currentBuffer();

        if (target instanceof VulkanGpuBuffer.Direct) {
            Std140Builder.intoBuffer(((PersistentRingBuffer) this.txoptimizations$levelProjection).txoptimizations$mapCurrent().clear()).putMat4f(projectionMatrix);
        } else {
            try (GpuBufferSlice.MappedView view = target.map(false, true)) {
                Std140Builder.intoBuffer(view.data()).putMat4f(projectionMatrix);
            }
        }

        GpuBufferSlice slice = target.slice(0L, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);

        LevelProjection.capture(projectionMatrix, slice);

        return slice;
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void txoptimizations$closeLevelProjection(CallbackInfo ci) {
        this.txoptimizations$levelProjection.close();
    }
}
