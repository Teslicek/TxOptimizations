package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.teslicek.txoptimizations.DrawRecords;
import com.teslicek.txoptimizations.FacingCulling;
import com.teslicek.txoptimizations.PackedTerrain;
import com.teslicek.txoptimizations.SectionDrawList;
import com.teslicek.txoptimizations.SectionDrawRecord;
import com.teslicek.txoptimizations.SectionDrawUniforms;
import com.teslicek.txoptimizations.SectionLayers;
import com.teslicek.txoptimizations.gpu.ProfileCounters;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = LevelRenderer.class, priority = 999)
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
    private static final int CHUNK = 64;

    @Unique
    private final SectionDrawList txoptimizations$draws = new SectionDrawList();

    @Unique
    private final SectionRenderDispatcher.RenderSection[] txoptimizations$chunkSections = new SectionRenderDispatcher.RenderSection[CHUNK];

    @Unique
    private final int[][] txoptimizations$chunkRecords = new int[CHUNK][];

    @Unique
    private final int[] txoptimizations$chunkFlags = new int[CHUNK];

    @Unique
    private final int[] txoptimizations$chunkX = new int[CHUNK];

    @Unique
    private final int[] txoptimizations$chunkY = new int[CHUNK];

    @Unique
    private final int[] txoptimizations$chunkZ = new int[CHUNK];

    @Unique
    private final float[] txoptimizations$chunkFades = new float[CHUNK];

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
            List<ChunkSectionsToRender.GpuMultiDrawIndexedIndirect> layerDraws = new ArrayList<>(draws.layerEnd(layer) - index);

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
        boolean[]       translucent = SectionLayers.TRANSLUCENT;
        SectionDrawList draws       = this.txoptimizations$draws;

        draws.begin(translucent.length, respectTranslucentOrder);

        if (this.sectionRenderDispatcher == null)
            return draws.finish();

        FacingCulling.beginFrame(this.levelRenderState.cameraRenderState.pos);

        long fadeDuration = Util.toMillis(this.optionsRenderState.chunkSectionFadeInTime);
        long now          = Util.getMillis();

        SectionRenderDispatcher.RenderSection[] chunkSections = this.txoptimizations$chunkSections;
        int[][]                                 chunkRecords  = this.txoptimizations$chunkRecords;
        int[]                                   chunkFlags    = this.txoptimizations$chunkFlags;
        int[]                                   chunkX        = this.txoptimizations$chunkX;
        int[]                                   chunkY        = this.txoptimizations$chunkY;
        int[]                                   chunkZ        = this.txoptimizations$chunkZ;
        float[]                                 chunkFades    = this.txoptimizations$chunkFades;
        Object[]                                visible       = this.visibleSections.elements();
        int                                     size          = this.visibleSections.size();

        for (int chunkStart = 0; chunkStart < size; chunkStart += CHUNK) {
            int chunkSize = Math.min(CHUNK, size - chunkStart);
            int drawn     = 0;

            for (int index = 0; index < chunkSize; index ++) {
                SectionRenderDispatcher.RenderSection section = (SectionRenderDispatcher.RenderSection) visible[chunkStart + index];

                chunkSections[index] = section;
                chunkRecords[index]  = ((SectionDrawRecord) section).txoptimizations$sectionDrawRecord();
            }

            for (int index = 0; index < chunkSize; index ++) {
                int[] record = chunkRecords[index];

                if (record == null)
                    continue;

                int flags = DrawRecords.flags(record);

                if (DrawRecords.drawable(flags) == 0)
                    continue;

                SectionRenderDispatcher.RenderSection section      = chunkSections[index];
                BlockPos                              renderOffset = section.getRenderOrigin();

                chunkRecords[drawn] = record;
                chunkFlags[drawn]   = flags;
                chunkX[drawn]       = renderOffset.getX();
                chunkY[drawn]       = renderOffset.getY();
                chunkZ[drawn]       = renderOffset.getZ();
                chunkFades[drawn]   = section.getVisibility(now, fadeDuration);
                drawn ++;
            }

            for (int index = 0; index < drawn; index ++)
                txoptimizations$collectSection(draws, translucent, chunkRecords[index], chunkFlags[index], chunkX[index], chunkY[index], chunkZ[index], chunkFades[index]);
        }

        Arrays.fill(chunkSections, null);
        Arrays.fill(chunkRecords, null);
        draws.finish();
        ProfileCounters.countTerrainDraws(draws);
        PackedTerrain.setSectionsFading(draws.fading());

        return draws;
    }

    @Unique
    private static void txoptimizations$collectSection(SectionDrawList draws, boolean[] translucent, int[] record, int flags, int originX, int originY, int originZ, float visibility) {
        int     drawable    = DrawRecords.drawable(flags);
        int     sectionInfo = draws.addSection(originX, originY, originZ, visibility);
        boolean prepared    = false;

        for (int layer = 0; layer < translucent.length; layer ++) {
            if ((drawable & 1 << layer) == 0)
                continue;

            int block        = DrawRecords.block(flags, layer);
            int vertexBuffer = record[block + DrawRecords.VERTEX_BUFFER];
            int firstVertex  = record[block + DrawRecords.FIRST_VERTEX];
            int indexCount   = record[block + DrawRecords.INDEX_COUNT];

            if (!translucent[layer]) {
                draws.shareIndexBuffer(indexCount);

                if (!prepared) {
                    FacingCulling.prepareSection(originX, originY, originZ);
                    prepared = true;
                }

                int facing        = block + DrawRecords.FACING;
                int visibleGroups = FacingCulling.visibleGroups(record, facing);

                if (visibleGroups == 0)
                    continue;

                FacingCulling.addDraws(draws, draws.group(layer, false, vertexBuffer, DrawRecords.NO_BUFFER, 0), record, facing, visibleGroups, firstVertex, sectionInfo);
                continue;
            }

            int format = record[block + DrawRecords.INDEX_FORMAT];

            if (!DrawRecords.customIndex(format)) {
                draws.shareIndexBuffer(indexCount);
                draws.add(draws.group(layer, true, vertexBuffer, DrawRecords.NO_BUFFER, 0), indexCount, 0, firstVertex, sectionInfo);
                continue;
            }

            draws.add(draws.group(layer, true, vertexBuffer, record[block + DrawRecords.INDEX_BUFFER], DrawRecords.indexType(format)), indexCount, record[block + DrawRecords.FIRST_INDEX], firstVertex, sectionInfo);
        }
    }
}
