package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.IndexType;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import net.caffeinemc.mods.sodium.client.gpu.device.batch.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.SharedQuadIndexBuffer;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataUnsafe;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DefaultChunkRenderer.class, remap = false)
public abstract class DefaultChunkRendererShortIndexMixin {

    @Unique
    private static final int SHORT_QUADS = 16383;

    @Unique
    private static final int SHORT_ELEMENTS = SHORT_QUADS * 6;

    @Unique
    private static boolean txoptimizations$offsetSeen;

    @Unique
    private GpuBuffer txoptimizations$shortIndices;

    @Unique
    private int txoptimizations$maxElements;

    @Inject(method = "prepare", at = @At("HEAD"))
    private void txoptimizations$resetMaxElements(CallbackInfo ci) {
        if (this.txoptimizations$shortIndices == null)
            this.txoptimizations$shortIndices = txoptimizations$createShortIndices();

        this.txoptimizations$maxElements = 0;
    }

    @WrapOperation(method = "prepare", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/SharedQuadIndexBuffer;ensureCapacity(I)V"))
    private void txoptimizations$recordMaxElements(SharedQuadIndexBuffer buffer, int elementCount, Operation<Void> original) {
        this.txoptimizations$maxElements = Math.max(this.txoptimizations$maxElements, elementCount);
        original.call(buffer, elementCount);
    }

    @WrapOperation(method = "fillCommandBuffer", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/DefaultChunkRenderer;addSharedIndexedDrawCommands(Lnet/caffeinemc/mods/sodium/client/gpu/device/batch/MultiDrawBatch;JI)V"))
    private static void txoptimizations$watchOffsets(MultiDrawBatch batch, long pMeshData, int mask, Operation<Void> original, @Local(argsOnly = true) TerrainRenderPass pass) {
        if (!pass.isTranslucent() && SectionRenderDataUnsafe.getBaseElement(pMeshData) != 0L)
            txoptimizations$offsetSeen = true;

        original.call(batch, pMeshData, mask);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/RenderPass;setIndexBuffer(Lcom/mojang/renderpearl/api/buffers/GpuBuffer;Lcom/mojang/renderpearl/api/pipeline/IndexType;)V", ordinal = 0))
    private void txoptimizations$bindShortIndices(RenderPass pass, GpuBuffer buffer, IndexType type, Operation<Void> original, @Local(argsOnly = true) TerrainRenderPass renderPass) {
        if (renderPass.isTranslucent() || txoptimizations$offsetSeen || this.txoptimizations$maxElements > SHORT_ELEMENTS) {
            original.call(pass, buffer, type);

            return;
        }

        original.call(pass, this.txoptimizations$shortIndices, IndexType.SHORT);
    }

    @Inject(method = "delete", at = @At("TAIL"))
    private void txoptimizations$deleteShortIndices(CallbackInfo ci) {
        if (this.txoptimizations$shortIndices != null)
            this.txoptimizations$shortIndices.close();
    }

    @Unique
    private static GpuBuffer txoptimizations$createShortIndices() {
        ByteBuffer data = MemoryUtil.memAlloc(SHORT_ELEMENTS * Short.BYTES).order(ByteOrder.nativeOrder());

        for (int quad = 0; quad < SHORT_QUADS; quad ++) {
            int vertex = quad * 4;

            data.putShort((short) vertex);
            data.putShort((short) (vertex + 1));
            data.putShort((short) (vertex + 2));
            data.putShort((short) (vertex + 2));
            data.putShort((short) (vertex + 3));
            data.putShort((short) vertex);
        }

        data.flip();

        try {
            return RenderSystem.getDevice().createBuffer(() -> "Shared short index buffer", GpuBuffer.USAGE_INDEX, data);
        } finally {
            MemoryUtil.memFree(data);
        }
    }
}
