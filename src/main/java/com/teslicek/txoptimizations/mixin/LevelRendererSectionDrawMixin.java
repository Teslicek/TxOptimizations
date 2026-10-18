package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.TlsfAllocator;
import com.mojang.blaze3d.vertex.UberGpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.teslicek.txoptimizations.FacingCulling;
import com.teslicek.txoptimizations.SectionAllocations;
import com.teslicek.txoptimizations.SectionDrawList;
import com.teslicek.txoptimizations.SectionDrawUniforms;
import com.teslicek.txoptimizations.SectionFacing;
import com.teslicek.txoptimizations.SectionLayers;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import org.joml.Matrix4fc;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererSectionDrawMixin {

    @Shadow
    @Final
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

    @Shadow
    @Final
    private OptionsRenderState optionsRenderState;

    @Shadow
    @Final
    private LevelRenderState levelRenderState;

    @Shadow
    @Final
    private TextureManager textureManager;

    @Shadow
    private @Nullable SectionRenderDispatcher sectionRenderDispatcher;

    @Unique
    private final SectionDrawList txoptimizations$draws = new SectionDrawList();

    @Unique
    private int[] txoptimizations$vertexSizes = new int[0];

    @Overwrite
    public ChunkSectionsToRender prepareChunkRendersIndirect(Matrix4fc modelViewMatrix, boolean respectTranslucentOrder) {
        GpuTextureView      blockAtlas         = this.textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        int                 textureAtlasWidth  = blockAtlas.getWidth(0);
        int                 textureAtlasHeight = blockAtlas.getHeight(0);
        SectionDrawList     draws              = this.txoptimizations$collectDraws(respectTranslucentOrder);
        DynamicGpuData      dynamicUniforms    = RenderSystem.getDynamicUniforms();
        GpuBufferSlice[]    commandBuffers     = ((SectionDrawUniforms) dynamicUniforms).txoptimizations$writeSectionCommands(draws);
        ChunkSectionLayer[] layers             = SectionLayers.VALUES;
        int                 index              = 0;

        EnumMap<ChunkSectionLayer, List<ChunkSectionsToRender.GpuMultiDrawIndexedIndirect>> indirectDraws = new EnumMap<>(ChunkSectionLayer.class);

        for (int layer = 0; layer < layers.length; layer ++) {
            List<ChunkSectionsToRender.GpuMultiDrawIndexedIndirect> layerDraws = new ArrayList<>();

            for (; index < draws.layerEnd(layer); index ++) {
                int       group       = draws.outputGroup(index);
                GpuBuffer indexBuffer = draws.indexBuffer(group);

                layerDraws.add(new ChunkSectionsToRender.GpuMultiDrawIndexedIndirect(draws.vertexBuffer(group).slice(), indexBuffer != null ? indexBuffer.slice() : null, draws.indexType(group), commandBuffers[index], draws.drawCount(group)));
            }

            indirectDraws.put(layers[layer], layerDraws);
        }

        GpuBufferSlice terrainTransform  = dynamicUniforms.writeTerrainTransform(modelViewMatrix, textureAtlasWidth, textureAtlasHeight);
        GpuBufferSlice chunkSectionInfos = ((SectionDrawUniforms) dynamicUniforms).txoptimizations$writeSectionInfos(draws);
        int            largestIndexCount = draws.largestIndexCount();

        RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);

        if (largestIndexCount != 0)
            autoIndices.requestIndexCount(largestIndexCount);

        return new ChunkSectionsToRender.DrawIndirect(terrainTransform, indirectDraws, largestIndexCount, chunkSectionInfos);
    }

    @Overwrite
    private int extractSectionDrawGroups(boolean respectTranslucentOrder, List<DynamicGpuData.ChunkSectionInfo> sectionInfos, Map<ChunkSectionLayer, List<LevelRenderer.ChunkDrawGroup>> drawGroups) {
        SectionDrawList     draws  = this.txoptimizations$collectDraws(respectTranslucentOrder);
        ChunkSectionLayer[] layers = SectionLayers.VALUES;

        for (int section = 0; section < draws.sectionCount(); section ++)
            sectionInfos.add(draws.sectionInfo(section));

        for (int layer = 0; layer < layers.length; layer ++) {
            List<LevelRenderer.ChunkDrawGroup> layerGroups = drawGroups.get(layers[layer]);

            for (int index = 0; index < draws.layerGroupCount(layer); index ++) {
                int       group       = draws.layerGroup(layer, index);
                GpuBuffer indexBuffer = draws.indexBuffer(group);

                layerGroups.add(new LevelRenderer.ChunkDrawGroup(draws.vertexBuffer(group).slice(), indexBuffer != null ? indexBuffer.slice() : null, draws.indexType(group), draws.indexedDraws(group)));
            }
        }

        return draws.largestIndexCount();
    }

    @Unique
    private SectionDrawList txoptimizations$collectDraws(boolean respectTranslucentOrder) {
        ChunkSectionLayer[]     layers     = SectionLayers.VALUES;
        SectionDrawList         draws      = this.txoptimizations$draws;
        SectionRenderDispatcher dispatcher = this.sectionRenderDispatcher;

        draws.begin(layers.length, respectTranslucentOrder);

        if (dispatcher == null)
            return draws.finish();

        if (this.txoptimizations$vertexSizes.length != layers.length)
            this.txoptimizations$vertexSizes = new int[layers.length];

        int[] vertexSizes = this.txoptimizations$vertexSizes;

        for (int layerIndex = 0; layerIndex < layers.length; layerIndex ++)
            vertexSizes[layerIndex] = layers[layerIndex].pipeline(false).getVertexFormatBinding(0).getVertexSize();

        CameraRenderState camera = this.levelRenderState.cameraRenderState;
        Vector3dc         eye    = FacingCulling.eye(camera.pos, camera.viewRotationMatrix);

        dispatcher.lock();

        long fadeDuration = Util.toMillis(this.optionsRenderState.chunkSectionFadeInTime);
        long now          = Util.getMillis();

        try {
            for (SectionRenderDispatcher.RenderSection section : this.visibleSections) {
                SectionMesh sectionMesh = section.getSectionMesh();

                if (!(sectionMesh instanceof SectionAllocations allocations))
                    continue;

                SectionFacing facing               = (SectionFacing) sectionMesh;
                BlockPos      renderOffset         = section.getRenderOrigin();
                double        eyeX                 = eye.x() - renderOffset.getX();
                double        eyeY                 = eye.y() - renderOffset.getY();
                double        eyeZ                 = eye.z() - renderOffset.getZ();
                int           sectionInfoDataIndex = -1;

                for (int layerIndex = 0; layerIndex < layers.length; layerIndex ++) {
                    ChunkSectionLayer        layer            = layers[layerIndex];
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

                    if (sectionInfoDataIndex == -1)
                        sectionInfoDataIndex = draws.addSection(renderOffset.getX(), renderOffset.getY(), renderOffset.getZ(), section.getVisibility(now, fadeDuration));

                    GpuBuffer vertexBuffer = txoptimizations$gpuBuffer(vertexAllocation);
                    int       baseVertex   = (int) vertexAllocation.getOffsetFromHeap() / vertexSizes[layerIndex];

                    if (!layer.translucent()) {
                        if (indexAllocation != null)
                            throw new IllegalStateException(layer.label() + " geometry of the section at " + renderOffset + " has its own index buffer, but its facing groups index the shared quad buffer");

                        draws.shareIndexBuffer(draw.indexCount());

                        int[] ranges = facing.txoptimizations$facingRanges(layer);

                        if (ranges == null)
                            throw new IllegalStateException(layer.label() + " geometry of the section at " + renderOffset + " was never grouped by facing");

                        int visibleGroups = FacingCulling.visibleGroups(ranges, eyeX, eyeY, eyeZ);

                        if (visibleGroups == 0)
                            continue;

                        FacingCulling.addDraws(draws, draws.group(layerIndex, false, vertexBuffer, null, null), ranges, visibleGroups, baseVertex, sectionInfoDataIndex);
                        continue;
                    }

                    if (indexAllocation == null) {
                        draws.shareIndexBuffer(draw.indexCount());
                        draws.add(draws.group(layerIndex, true, vertexBuffer, null, null), draw.indexCount(), 0, baseVertex, sectionInfoDataIndex);
                        continue;
                    }

                    int firstIndex = (int) indexAllocation.getOffsetFromHeap() / draw.indexType().bytes;

                    draws.add(draws.group(layerIndex, true, vertexBuffer, txoptimizations$gpuBuffer(indexAllocation), draw.indexType()), draw.indexCount(), firstIndex, baseVertex, sectionInfoDataIndex);
                }
            }
        } finally {
            dispatcher.unlock();
        }

        return draws.finish();
    }

    @Unique
    private static GpuBuffer txoptimizations$gpuBuffer(TlsfAllocator.Allocation allocation) {
        return ((UberGpuBuffer.UberGpuBufferHeap) allocation.getHeap()).gpuBuffer;
    }
}
