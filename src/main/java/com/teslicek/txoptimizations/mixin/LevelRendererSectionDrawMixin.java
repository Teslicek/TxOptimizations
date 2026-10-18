package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.TlsfAllocator;
import com.mojang.blaze3d.vertex.UberGpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.teslicek.txoptimizations.SectionAllocations;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererSectionDrawMixin {

    @Unique
    private static final ChunkSectionLayer[] LAYERS = ChunkSectionLayer.values();

    @Unique
    private static final int GROUP_HASH_SEED = 173;

    @Shadow
    @Final
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

    @Shadow
    @Final
    private OptionsRenderState optionsRenderState;

    @Shadow
    private @Nullable SectionRenderDispatcher sectionRenderDispatcher;

    @Overwrite
    private int extractSectionDrawGroups(boolean respectTranslucentOrder, List<DynamicGpuData.ChunkSectionInfo> sectionInfos, Map<ChunkSectionLayer, List<LevelRenderer.ChunkDrawGroup>> drawGroups) {
        SectionRenderDispatcher dispatcher = this.sectionRenderDispatcher;

        if (dispatcher == null)
            return 0;

        int[] vertexSizes = new int[LAYERS.length];

        for (int layerIndex = 0; layerIndex < LAYERS.length; layerIndex ++)
            vertexSizes[layerIndex] = LAYERS[layerIndex].pipeline(false).getVertexFormatBinding(0).getVertexSize();

        Int2ObjectOpenHashMap<LevelRenderer.ChunkDrawGroup> drawGroupCache           = new Int2ObjectOpenHashMap<>();
        int                                                 largestIndexCount        = 0;
        int                                                 lastTransparentGroupHash = 0;

        dispatcher.lock();

        long fadeDuration = Util.toMillis(this.optionsRenderState.chunkSectionFadeInTime);
        long now          = Util.getMillis();

        try {
            for (SectionRenderDispatcher.RenderSection section : this.visibleSections) {
                SectionMesh sectionMesh = section.getSectionMesh();

                if (!(sectionMesh instanceof SectionAllocations allocations))
                    continue;

                int sectionInfoDataIndex = -1;

                for (int layerIndex = 0; layerIndex < LAYERS.length; layerIndex ++) {
                    ChunkSectionLayer        layer            = LAYERS[layerIndex];
                    TlsfAllocator.Allocation vertexAllocation = allocations.txoptimizations$allocation(SectionAllocations.vertexSlot(layer));

                    if (vertexAllocation == null)
                        continue;

                    SectionMesh.SectionDraw draw = sectionMesh.getSectionDraw(layer);

                    if (draw == null)
                        continue;

                    TlsfAllocator.Allocation indexAllocation = null;

                    if (draw.hasCustomIndexBuffer()) {
                        indexAllocation = allocations.txoptimizations$allocation(SectionAllocations.indexSlot(layer));

                        if (indexAllocation == null)
                            continue;
                    }

                    if (sectionInfoDataIndex == -1) {
                        BlockPos renderOffset = section.getRenderOrigin();

                        sectionInfoDataIndex = sectionInfos.size();
                        sectionInfos.add(new DynamicGpuData.ChunkSectionInfo(renderOffset.getX(), renderOffset.getY(), renderOffset.getZ(), section.getVisibility(now, fadeDuration)));
                    }

                    GpuBuffer vertexBuffer = txoptimizations$gpuBuffer(vertexAllocation);
                    GpuBuffer indexBuffer  = null;
                    IndexType indexType    = null;
                    int       combinedHash = 31 * GROUP_HASH_SEED + vertexBuffer.hashCode();
                    int       firstIndex   = 0;

                    if (indexAllocation == null) {
                        if (draw.indexCount() > largestIndexCount)
                            largestIndexCount = draw.indexCount();
                    } else {
                        indexBuffer  = txoptimizations$gpuBuffer(indexAllocation);
                        indexType    = draw.indexType();
                        combinedHash = 31 * combinedHash + indexBuffer.hashCode();
                        combinedHash = 31 * combinedHash + indexType.hashCode();
                        firstIndex   = (int) indexAllocation.getOffsetFromHeap() / indexType.bytes;
                    }

                    int                          baseVertex = (int) vertexAllocation.getOffsetFromHeap() / vertexSizes[layerIndex];
                    LevelRenderer.ChunkDrawGroup drawGroup  = null;

                    if (layer.translucent() && respectTranslucentOrder) {
                        if (combinedHash == lastTransparentGroupHash)
                            drawGroup = drawGroups.get(layer).getLast();

                        lastTransparentGroupHash = combinedHash;
                    } else {
                        drawGroup = drawGroupCache.getOrDefault(combinedHash, null);
                    }

                    if (drawGroup == null) {
                        drawGroup = new LevelRenderer.ChunkDrawGroup(vertexBuffer.slice(), indexBuffer != null ? indexBuffer.slice() : null, indexType, new ArrayList<>());
                        drawGroupCache.put(combinedHash, drawGroup);
                        drawGroups.get(layer).add(drawGroup);
                    }

                    drawGroup.draws().add(new DynamicGpuData.IndexedDraw(draw.indexCount(), 1, firstIndex, baseVertex, sectionInfoDataIndex));
                }
            }
        } finally {
            dispatcher.unlock();
        }

        return largestIndexCount;
    }

    @Unique
    private static GpuBuffer txoptimizations$gpuBuffer(TlsfAllocator.Allocation allocation) {
        return ((UberGpuBuffer.UberGpuBufferHeap) allocation.getHeap()).gpuBuffer;
    }
}
