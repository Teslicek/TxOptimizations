package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.TlsfAllocator;
import com.teslicek.txoptimizations.SectionAllocations;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CompiledSectionMesh.class)
public abstract class CompiledSectionMeshAllocationMixin implements SectionAllocations {

    @Unique
    private final TlsfAllocator.@Nullable Allocation[] txoptimizations$allocations = new TlsfAllocator.Allocation[SectionAllocations.SLOTS];

    @Override
    public TlsfAllocator.@Nullable Allocation txoptimizations$allocation(int slot) {
        return this.txoptimizations$allocations[slot];
    }

    @Override
    public void txoptimizations$setAllocation(int slot, TlsfAllocator.@Nullable Allocation allocation) {
        this.txoptimizations$allocations[slot] = allocation;
    }
}
