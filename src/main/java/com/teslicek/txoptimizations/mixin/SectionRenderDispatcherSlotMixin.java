package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SectionAllocations;
import com.teslicek.txoptimizations.UberBufferSlot;
import java.util.Map;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionRenderDispatcher.class)
public abstract class SectionRenderDispatcherSlotMixin {

    @Shadow
    @Final
    private Map<ChunkSectionLayer, SectionRenderDispatcher.SectionUberBuffers> chunkUberBuffers;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$assignMeshSlots(CallbackInfo ci) {
        if (ChunkSectionLayer.values().length * 2 != SectionAllocations.SLOTS)
            throw new IllegalStateException("Section allocations hold " + SectionAllocations.SLOTS + " slots but there are " + ChunkSectionLayer.values().length + " section layers");

        this.chunkUberBuffers.forEach((layer, buffers) -> {
            ((UberBufferSlot) buffers.vertexBuffer()).txoptimizations$setSlot(SectionAllocations.vertexSlot(layer));
            ((UberBufferSlot) buffers.indexBuffer()).txoptimizations$setSlot(SectionAllocations.indexSlot(layer));
        });
    }
}
