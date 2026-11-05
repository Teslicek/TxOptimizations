package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.StagingBuffer;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.teslicek.txoptimizations.CompactTerrain;
import com.teslicek.txoptimizations.SectionAllocations;
import com.teslicek.txoptimizations.SectionLayers;
import com.teslicek.txoptimizations.SectionMeshReleases;
import com.teslicek.txoptimizations.UberBufferSlot;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionRenderDispatcher.class)
public abstract class SectionRenderDispatcherMixin implements SectionMeshReleases {

    @ModifyExpressionValue(method = "lambda$new$0", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/vertex/VertexFormat;getVertexSize()I"), require = 1)
    private int txoptimizations$compactAlignment(int vertexSize) {
        return CompactTerrain.bufferAlignment(vertexSize);
    }

    @Unique
    private final ConcurrentLinkedQueue<Runnable> txoptimizations$releases = new ConcurrentLinkedQueue<>();

    @Override
    public void txoptimizations$releaseOnRenderThread(Runnable release) {
        this.txoptimizations$releases.add(release);
    }

    @Redirect(method = "uploadTerrainBuffersToGpu", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/StagingBuffer;startUploading(Lcom/mojang/renderpearl/api/commands/CommandEncoder;)Lcom/mojang/blaze3d/vertex/StagingBuffer$Uploader;"))
    private StagingBuffer.Uploader txoptimizations$releaseBeforeUploading(StagingBuffer stagingBuffer, CommandEncoder encoder) {
        Runnable release;

        while ((release = this.txoptimizations$releases.poll()) != null)
            release.run();

        return stagingBuffer.startUploading(encoder);
    }

    @Shadow
    @Final
    private Map<ChunkSectionLayer, SectionRenderDispatcher.SectionUberBuffers> chunkUberBuffers;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$assignMeshSlots(CallbackInfo ci) {
        if (SectionAllocations.SLOTS != SectionLayers.VALUES.length * 2)
            throw new IllegalStateException("Meshes keep " + SectionAllocations.SLOTS + " allocation slots but " + SectionLayers.VALUES.length + " section layers need " + SectionLayers.VALUES.length * 2);

        this.chunkUberBuffers.forEach((layer, buffers) -> {
            ((UberBufferSlot) buffers.vertexBuffer()).txoptimizations$setSlot(SectionAllocations.vertexSlot(layer));
            ((UberBufferSlot) buffers.indexBuffer()).txoptimizations$setSlot(SectionAllocations.indexSlot(layer));
        });
    }
}
