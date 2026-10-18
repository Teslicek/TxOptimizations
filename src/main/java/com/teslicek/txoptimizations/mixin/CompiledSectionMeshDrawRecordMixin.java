package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.TlsfAllocator;
import com.teslicek.txoptimizations.DrawRecordHolder;
import com.teslicek.txoptimizations.DrawRecords;
import com.teslicek.txoptimizations.FacingCulling;
import com.teslicek.txoptimizations.SectionAllocations;
import com.teslicek.txoptimizations.SectionBufferId;
import com.teslicek.txoptimizations.SectionLayers;
import java.util.Map;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CompiledSectionMesh.class)
public abstract class CompiledSectionMeshDrawRecordMixin implements SectionAllocations, DrawRecordHolder {

    @Shadow
    @Final
    private Map<ChunkSectionLayer, SectionMesh.SectionDraw> draws;

    @Unique
    private final TlsfAllocator.@Nullable Allocation[] txoptimizations$allocations = new TlsfAllocator.Allocation[SectionAllocations.SLOTS];

    @Unique
    private int @Nullable [] txoptimizations$drawRecord;

    @Unique
    private boolean txoptimizations$translucentGeometry;

    @Inject(method = "<init>(Lnet/minecraft/client/renderer/chunk/TranslucencyPointOfView;Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;J)V", at = @At("RETURN"))
    private void txoptimizations$createDrawRecord(TranslucencyPointOfView translucencyPointOfView, SectionCompiler.Results results, long compileTaskStartTimeNs, CallbackInfo ci) {
        this.txoptimizations$translucentGeometry = this.draws.containsKey(ChunkSectionLayer.TRANSLUCENT);

        int rendered = 0;

        for (ChunkSectionLayer layer : results.renderedLayers.keySet())
            rendered |= 1 << layer.ordinal();

        if (rendered == 0)
            return;

        int[]   record = DrawRecords.create(rendered, SectionLayers.TRANSLUCENT);
        float[] bounds = FacingCulling.emptyBounds();
        boolean opaque = false;

        for (Map.Entry<ChunkSectionLayer, MeshData> entry : results.renderedLayers.entrySet()) {
            ChunkSectionLayer       layer = entry.getKey();
            SectionMesh.SectionDraw draw  = this.draws.get(layer);

            if (draw == null)
                throw new IllegalStateException(layer.label() + " geometry was meshed but the section mesh holds no draw for it");

            DrawRecords.setDraw(record, layer.ordinal(), layer.translucent(), draw.indexCount(), draw.hasCustomIndexBuffer(), draw.indexType().ordinal());

            if (layer.translucent())
                continue;

            FacingCulling.groupQuads(entry.getValue(), bounds, record, DrawRecords.facing(record, layer.ordinal()));
            opaque = true;
        }

        if (opaque)
            DrawRecords.setBounds(record, bounds);

        this.txoptimizations$drawRecord = record;
    }

    @Overwrite
    public boolean hasTranslucentGeometry() {
        return this.txoptimizations$translucentGeometry;
    }

    @Override
    public int @Nullable [] txoptimizations$drawRecord() {
        return this.txoptimizations$drawRecord;
    }

    @Override
    public TlsfAllocator.@Nullable Allocation txoptimizations$allocation(int slot) {
        return this.txoptimizations$allocations[slot];
    }

    @Override
    public void txoptimizations$setAllocation(int slot, TlsfAllocator.@Nullable Allocation allocation) {
        this.txoptimizations$allocations[slot] = allocation;

        int[] record = this.txoptimizations$drawRecord;

        if (record == null) {
            if (allocation != null)
                throw new IllegalStateException("A section mesh without geometry was given buffer space in slot " + slot);

            return;
        }

        this.txoptimizations$refresh(record, SectionLayers.VALUES[SectionAllocations.layer(slot)]);
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void txoptimizations$dropDraws(CallbackInfo ci) {
        this.txoptimizations$translucentGeometry = false;

        int[] record = this.txoptimizations$drawRecord;

        if (record == null)
            return;

        for (ChunkSectionLayer layer : SectionLayers.VALUES) {
            if (DrawRecords.rendered(DrawRecords.flags(record), layer.ordinal()))
                this.txoptimizations$refresh(record, layer);
        }
    }

    @Unique
    private void txoptimizations$refresh(int[] record, ChunkSectionLayer layer) {
        TlsfAllocator.Allocation vertex = this.txoptimizations$allocations[SectionAllocations.vertexSlot(layer)];
        TlsfAllocator.Allocation index  = this.txoptimizations$allocations[SectionAllocations.indexSlot(layer)];
        SectionMesh.SectionDraw  draw   = this.draws.get(layer);

        if (vertex == null || draw == null || draw.hasCustomIndexBuffer() && index == null) {
            DrawRecords.clearDrawable(record, layer.ordinal(), layer.translucent());
            return;
        }

        int indexBuffer = DrawRecords.NO_BUFFER;
        int firstIndex  = 0;

        if (draw.hasCustomIndexBuffer()) {
            indexBuffer = txoptimizations$bufferId(index);
            firstIndex  = (int) index.getOffsetFromHeap() / draw.indexType().bytes;
        }

        DrawRecords.setDrawable(record, layer.ordinal(), txoptimizations$bufferId(vertex), (int) vertex.getOffsetFromHeap() / layer.pipeline(false).getVertexFormatBinding(0).getVertexSize(), indexBuffer, firstIndex, layer.translucent());
    }

    @Unique
    private static int txoptimizations$bufferId(TlsfAllocator.Allocation allocation) {
        return ((SectionBufferId) allocation.getHeap()).txoptimizations$bufferId();
    }
}
