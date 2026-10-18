package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.TlsfAllocator;
import com.mojang.blaze3d.vertex.UberGpuBuffer;
import com.teslicek.txoptimizations.SectionAllocations;
import com.teslicek.txoptimizations.UberBufferSlot;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Map;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(UberGpuBuffer.class)
public abstract class UberGpuBufferMeshAllocationMixin implements UberBufferSlot {

    @Unique
    private static final int NO_SLOT = -1;

    @Shadow
    @Final
    private String name;

    @Unique
    private final ReferenceOpenHashSet<SectionAllocations> txoptimizations$meshes = new ReferenceOpenHashSet<>();

    @Unique
    private int txoptimizations$slot = NO_SLOT;

    @Override
    public void txoptimizations$setSlot(int slot) {
        if (this.txoptimizations$slot != NO_SLOT)
            throw new IllegalStateException(this.name + " already holds mesh slot " + this.txoptimizations$slot);

        this.txoptimizations$slot = slot;
    }

    @Redirect(method = "uploadStagedAllocations", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object txoptimizations$storeOnMesh(Map<?, ?> allocationMap, Object key, Object allocation) {
        if (!(key instanceof SectionAllocations mesh))
            throw new IllegalStateException(this.name + " allocated buffer space for " + key + ", which has no allocation slots");

        int                      slot     = this.txoptimizations$requireSlot();
        TlsfAllocator.Allocation previous = mesh.txoptimizations$allocation(slot);

        mesh.txoptimizations$setAllocation(slot, (TlsfAllocator.Allocation) allocation);
        this.txoptimizations$meshes.add(mesh);

        return previous;
    }

    @Redirect(method = "getAllocation", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object txoptimizations$readFromMesh(Map<?, ?> allocationMap, Object key) {
        if (!(key instanceof SectionAllocations mesh))
            return null;

        return mesh.txoptimizations$allocation(this.txoptimizations$requireSlot());
    }

    @Redirect(method = "freeAllocation", at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object txoptimizations$removeFromMesh(Map<?, ?> allocationMap, Object key) {
        if (!(key instanceof SectionAllocations mesh))
            return null;

        int                      slot       = this.txoptimizations$requireSlot();
        TlsfAllocator.Allocation allocation = mesh.txoptimizations$allocation(slot);

        if (allocation == null)
            return null;

        mesh.txoptimizations$setAllocation(slot, null);
        this.txoptimizations$meshes.remove(mesh);

        return allocation;
    }

    @Redirect(method = "close", at = @At(value = "INVOKE", target = "Ljava/util/Map;clear()V"))
    private void txoptimizations$clearMeshes(Map<?, ?> allocationMap) {
        for (SectionAllocations mesh : this.txoptimizations$meshes)
            mesh.txoptimizations$setAllocation(this.txoptimizations$slot, null);

        this.txoptimizations$meshes.clear();
    }

    @Unique
    private int txoptimizations$requireSlot() {
        if (this.txoptimizations$slot == NO_SLOT)
            throw new IllegalStateException(this.name + " was never given a mesh slot");

        return this.txoptimizations$slot;
    }
}
