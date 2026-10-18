package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.TlsfAllocator;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionMesh;
import org.jspecify.annotations.Nullable;

public interface SectionAllocations {

    int SLOTS = 6;

    TlsfAllocator.@Nullable Allocation txoptimizations$allocation(int slot);

    void txoptimizations$setAllocation(int slot, TlsfAllocator.@Nullable Allocation allocation);

    SectionMesh.@Nullable SectionDraw txoptimizations$drawableDraw(int layer);

    @Nullable GpuBuffer txoptimizations$buffer(int slot);

    int txoptimizations$firstElement(int slot);

    static int vertexSlot(ChunkSectionLayer layer) {
        return layer.ordinal() * 2;
    }

    static int indexSlot(ChunkSectionLayer layer) {
        return layer.ordinal() * 2 + 1;
    }

    static int layer(int slot) {
        return slot / 2;
    }
}
