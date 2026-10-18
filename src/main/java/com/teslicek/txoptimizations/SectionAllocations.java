package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.TlsfAllocator;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.jspecify.annotations.Nullable;

public interface SectionAllocations {

    int SLOTS = 6;

    TlsfAllocator.@Nullable Allocation txoptimizations$allocation(int slot);

    void txoptimizations$setAllocation(int slot, TlsfAllocator.@Nullable Allocation allocation);

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
