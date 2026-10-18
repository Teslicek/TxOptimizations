package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.TlsfAllocator;
import com.mojang.blaze3d.vertex.UberGpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.teslicek.txoptimizations.SectionAllocations;
import com.teslicek.txoptimizations.SectionLayers;
import java.util.Map;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionMesh;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CompiledSectionMesh.class)
public abstract class CompiledSectionMeshAllocationMixin implements SectionAllocations {

    @Shadow
    @Final
    private Map<ChunkSectionLayer, SectionMesh.SectionDraw> draws;

    @Unique
    private final TlsfAllocator.@Nullable Allocation[] txoptimizations$allocations = new TlsfAllocator.Allocation[SectionAllocations.SLOTS];

    @Unique
    private final @Nullable GpuBuffer[] txoptimizations$buffers = new GpuBuffer[SectionAllocations.SLOTS];

    @Unique
    private final int[] txoptimizations$firstElements = new int[SectionAllocations.SLOTS];

    @Unique
    private final SectionMesh.@Nullable SectionDraw[] txoptimizations$drawableDraws = new SectionMesh.SectionDraw[SectionAllocations.SLOTS / 2];

    @Override
    public TlsfAllocator.@Nullable Allocation txoptimizations$allocation(int slot) {
        return this.txoptimizations$allocations[slot];
    }

    @Override
    public void txoptimizations$setAllocation(int slot, TlsfAllocator.@Nullable Allocation allocation) {
        this.txoptimizations$allocations[slot] = allocation;
        this.txoptimizations$refreshDraw(SectionLayers.VALUES[SectionAllocations.layer(slot)]);
    }

    @Override
    public SectionMesh.@Nullable SectionDraw txoptimizations$drawableDraw(int layer) {
        return this.txoptimizations$drawableDraws[layer];
    }

    @Override
    public @Nullable GpuBuffer txoptimizations$buffer(int slot) {
        return this.txoptimizations$buffers[slot];
    }

    @Override
    public int txoptimizations$firstElement(int slot) {
        return this.txoptimizations$firstElements[slot];
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void txoptimizations$dropDraws(CallbackInfo ci) {
        for (ChunkSectionLayer layer : SectionLayers.VALUES)
            this.txoptimizations$refreshDraw(layer);
    }

    @Unique
    private void txoptimizations$refreshDraw(ChunkSectionLayer layer) {
        int                      vertexSlot = SectionAllocations.vertexSlot(layer);
        int                      indexSlot  = SectionAllocations.indexSlot(layer);
        TlsfAllocator.Allocation vertex     = this.txoptimizations$allocations[vertexSlot];
        TlsfAllocator.Allocation index      = this.txoptimizations$allocations[indexSlot];
        SectionMesh.SectionDraw  draw       = this.draws.get(layer);

        this.txoptimizations$drawableDraws[layer.ordinal()] = null;
        this.txoptimizations$buffers[vertexSlot]            = null;
        this.txoptimizations$buffers[indexSlot]             = null;
        this.txoptimizations$firstElements[vertexSlot]      = 0;
        this.txoptimizations$firstElements[indexSlot]       = 0;

        if (vertex == null || draw == null)
            return;

        if (draw.hasCustomIndexBuffer()) {
            if (index == null)
                return;

            this.txoptimizations$buffers[indexSlot]       = txoptimizations$gpuBuffer(index);
            this.txoptimizations$firstElements[indexSlot] = (int) index.getOffsetFromHeap() / draw.indexType().bytes;
        }

        this.txoptimizations$buffers[vertexSlot]            = txoptimizations$gpuBuffer(vertex);
        this.txoptimizations$firstElements[vertexSlot]      = (int) vertex.getOffsetFromHeap() / layer.pipeline(false).getVertexFormatBinding(0).getVertexSize();
        this.txoptimizations$drawableDraws[layer.ordinal()] = draw;
    }

    @Unique
    private static GpuBuffer txoptimizations$gpuBuffer(TlsfAllocator.Allocation allocation) {
        return ((UberGpuBuffer.UberGpuBufferHeap) allocation.getHeap()).gpuBuffer;
    }
}
