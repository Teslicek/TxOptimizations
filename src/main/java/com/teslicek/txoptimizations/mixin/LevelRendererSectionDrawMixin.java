package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.teslicek.txoptimizations.DrawRecords;
import com.teslicek.txoptimizations.FacingCulling;
import com.teslicek.txoptimizations.FramePath;
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
import net.minecraft.core.SectionPos;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Unique
    private SectionRenderDispatcher.RenderSection[] txoptimizations$builtSections = new SectionRenderDispatcher.RenderSection[0];

    @Unique
    private int[][] txoptimizations$builtRecords = new int[0][];

    @Unique
    private int[] txoptimizations$builtVersions = new int[0];

    @Unique
    private int txoptimizations$builtSize = -1;

    @Unique
    private int txoptimizations$builtGeneration;

    @Unique
    private double txoptimizations$builtX;

    @Unique
    private double txoptimizations$builtY;

    @Unique
    private double txoptimizations$builtZ;

    @Unique
    private long txoptimizations$builtFadeDuration;

    @Unique
    private boolean txoptimizations$builtTranslucentOrder;

    @Unique
    private boolean txoptimizations$builtPerGroup;

    @Inject(method = "resetLevelRenderData", at = @At("HEAD"))
    private void txoptimizations$forgetDrawsOfOldLevel(CallbackInfo ci) {
        this.txoptimizations$forgetBuiltDraws();
    }

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

        if (this.sectionRenderDispatcher == null) {
            this.txoptimizations$forgetBuiltDraws();
            draws.begin(translucent.length, respectTranslucentOrder);

            return draws.finish();
        }

        Vec3    camera       = this.levelRenderState.cameraRenderState.pos;
        long    fadeDuration = Util.toMillis(this.optionsRenderState.chunkSectionFadeInTime);
        boolean perGroup     = FramePath.frameUsesWorker();
        int     generation   = DrawRecords.generation();

        if (this.txoptimizations$reusesBuiltDraws(draws, camera, fadeDuration, respectTranslucentOrder, perGroup, generation)) {
            ProfileCounters.countTerrainDraws(draws);
            PackedTerrain.setSectionsFading(false);

            return draws;
        }

        draws.begin(translucent.length, respectTranslucentOrder);
        FacingCulling.beginFrame(camera);

        long now = Util.getMillis();

        SectionRenderDispatcher.RenderSection[] chunkSections = this.txoptimizations$chunkSections;
        int[][]                                 chunkRecords  = this.txoptimizations$chunkRecords;
        int[]                                   chunkFlags    = this.txoptimizations$chunkFlags;
        int[]                                   chunkX        = this.txoptimizations$chunkX;
        int[]                                   chunkY        = this.txoptimizations$chunkY;
        int[]                                   chunkZ        = this.txoptimizations$chunkZ;
        float[]                                 chunkFades    = this.txoptimizations$chunkFades;
        Object[]                                visible       = this.visibleSections.elements();
        int                                     size          = this.visibleSections.size();
        SectionRenderDispatcher.RenderSection[] builtSections = this.txoptimizations$builtSections(size);
        int[][]                                 builtRecords  = this.txoptimizations$builtRecords;
        int[]                                   builtVersions = this.txoptimizations$builtVersions;

        for (int chunkStart = 0; chunkStart < size; chunkStart += CHUNK) {
            int chunkSize = Math.min(CHUNK, size - chunkStart);
            int drawn     = 0;

            for (int index = 0; index < chunkSize; index ++) {
                SectionRenderDispatcher.RenderSection section = (SectionRenderDispatcher.RenderSection) visible[chunkStart + index];

                chunkSections[index] = section;
                chunkRecords[index]  = ((SectionDrawRecord) section).txoptimizations$sectionDrawRecord();

                builtSections[chunkStart + index] = section;
                builtRecords[chunkStart + index]  = chunkRecords[index];
                builtVersions[chunkStart + index] = chunkRecords[index] == null ? 0 : DrawRecords.version(chunkRecords[index]);
            }

            for (int index = 0; index < chunkSize; index ++) {
                int[] record = chunkRecords[index];

                if (record == null)
                    continue;

                int flags = DrawRecords.flags(record);

                if (DrawRecords.drawable(flags) == 0)
                    continue;

                SectionRenderDispatcher.RenderSection section = chunkSections[index];
                long                                  node    = section.getSectionNode();

                chunkRecords[drawn] = record;
                chunkFlags[drawn]   = flags;
                chunkX[drawn]       = SectionPos.sectionToBlockCoord(SectionPos.x(node));
                chunkY[drawn]       = SectionPos.sectionToBlockCoord(SectionPos.y(node));
                chunkZ[drawn]       = SectionPos.sectionToBlockCoord(SectionPos.z(node));
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

        this.txoptimizations$builtSize             = size;
        this.txoptimizations$builtGeneration       = generation;
        this.txoptimizations$builtX                = camera.x;
        this.txoptimizations$builtY                = camera.y;
        this.txoptimizations$builtZ                = camera.z;
        this.txoptimizations$builtFadeDuration     = fadeDuration;
        this.txoptimizations$builtTranslucentOrder = respectTranslucentOrder;
        this.txoptimizations$builtPerGroup         = perGroup;

        return draws;
    }

    @Unique
    private boolean txoptimizations$reusesBuiltDraws(SectionDrawList draws, Vec3 camera, long fadeDuration, boolean respectTranslucentOrder, boolean perGroup, int generation) {
        int size = this.visibleSections.size();

        if (size != this.txoptimizations$builtSize || draws.fading() || generation != this.txoptimizations$builtGeneration || perGroup != this.txoptimizations$builtPerGroup || respectTranslucentOrder != this.txoptimizations$builtTranslucentOrder || fadeDuration != this.txoptimizations$builtFadeDuration)
            return false;

        if (camera.x != this.txoptimizations$builtX || camera.y != this.txoptimizations$builtY || camera.z != this.txoptimizations$builtZ)
            return false;

        Object[]                                visible       = this.visibleSections.elements();
        SectionRenderDispatcher.RenderSection[] builtSections = this.txoptimizations$builtSections;
        int[][]                                 builtRecords  = this.txoptimizations$builtRecords;
        int[]                                   builtVersions = this.txoptimizations$builtVersions;

        for (int index = 0; index < size; index ++) {
            SectionRenderDispatcher.RenderSection section = (SectionRenderDispatcher.RenderSection) visible[index];
            int[]                                 record  = ((SectionDrawRecord) section).txoptimizations$sectionDrawRecord();

            if (section != builtSections[index] || record != builtRecords[index] || record != null && DrawRecords.version(record) != builtVersions[index])
                return false;
        }

        return true;
    }

    @Unique
    private SectionRenderDispatcher.RenderSection[] txoptimizations$builtSections(int size) {
        this.txoptimizations$builtSize = -1;

        if (this.txoptimizations$builtSections.length < size) {
            int capacity = Math.max(size, this.txoptimizations$builtSections.length * 2);

            this.txoptimizations$builtSections = new SectionRenderDispatcher.RenderSection[capacity];
            this.txoptimizations$builtRecords  = new int[capacity][];
            this.txoptimizations$builtVersions = new int[capacity];
        }

        Arrays.fill(this.txoptimizations$builtSections, size, this.txoptimizations$builtSections.length, null);
        Arrays.fill(this.txoptimizations$builtRecords, size, this.txoptimizations$builtRecords.length, null);

        return this.txoptimizations$builtSections;
    }

    @Unique
    private void txoptimizations$forgetBuiltDraws() {
        this.txoptimizations$builtSize = -1;
        Arrays.fill(this.txoptimizations$builtSections, null);
        Arrays.fill(this.txoptimizations$builtRecords, null);
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
