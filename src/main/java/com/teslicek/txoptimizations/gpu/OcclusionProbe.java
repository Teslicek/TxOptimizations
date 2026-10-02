package com.teslicek.txoptimizations.gpu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.teslicek.txoptimizations.mixin.gpu.FrontendRenderPassAccessor;
import com.teslicek.txoptimizations.mixin.gpu.VulkanRenderPassAccessor;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.LocalSectionIndex;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataUnsafe;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.util.iterator.ByteIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueryPoolCreateInfo;

public final class OcclusionProbe {

    private static final int               PASSES         = DefaultTerrainRenderPasses.ALL.length;
    private static final int               BOX_VERTICES   = 36;
    private static final int               PUSH_CONSTANTS = 16;
    private static final double            NEAR_MARGIN    = 2.0;
    private static final double            SECTION_SIZE   = 16.0;
    private static final DateTimeFormatter FILE_TIME      = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");
    private static final BindGroupLayout   GLOBALS        = BindGroupLayout.builder().withUniform("u_Globals", UniformType.UNIFORM_BUFFER).build();
    private static final RenderPipeline    PIPELINE       = RenderPipeline.builder()
        .withLocation(Identifier.fromNamespaceAndPath("txoptimizations", "occlusion_probe"))
        .withBindGroupLayout(GLOBALS)
        .withPushConstantSize(PUSH_CONSTANTS)
        .withCull(false)
        .withVertexShader(Identifier.fromNamespaceAndPath("txoptimizations", "shaderc/occlusion_probe"))
        .withFragmentShader(Identifier.fromNamespaceAndPath("txoptimizations", "shaderc/occlusion_probe"))
        .withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM, 0))
        .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .build();

    private static boolean  armed;
    private static VkDevice device;
    private static long     queryPool = VK10.VK_NULL_HANDLE;
    private static int      queries;
    private static int      untestedSections;
    private static long[][] sectionVertices;
    private static long[]   untestedVertices;

    private OcclusionProbe() {
    }

    public static void arm() {
        if (isActive())
            throw new IllegalStateException("Occlusion probe is already running");

        armed = true;
    }

    public static boolean isActive() {
        return armed || queryPool != VK10.VK_NULL_HANDLE;
    }

    public static void onOpaqueTerrainDrawn(RenderPass pass, ChunkRenderListIterable renderLists, GpuBufferSlice globals, double x, double y, double z) {
        if (queryPool != VK10.VK_NULL_HANDLE) {
            report();

            return;
        }

        armed = false;
        record(pass, renderLists, globals, new CameraTransform(x, y, z));
    }

    private static void record(RenderPass pass, ChunkRenderListIterable renderLists, GpuBufferSlice globals, CameraTransform camera) {
        VulkanRenderPassAccessor backend       = (VulkanRenderPassAccessor) ((FrontendRenderPassAccessor) pass).txoptimizations$backend();
        VkCommandBuffer          commandBuffer = backend.txoptimizations$commandBuffer();
        int                      sections      = 0;

        for (ChunkRenderList list : iterable(renderLists))
            sections += list.getSectionsWithGeometryCount();

        device           = backend.txoptimizations$device().vkDevice();
        queryPool        = createQueryPool(sections);
        queries          = 0;
        untestedSections = 0;
        sectionVertices  = new long[sections][];
        untestedVertices = new long[PASSES];

        pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
        pass.setUniform("u_Globals", globals);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer constants = stack.malloc(PUSH_CONSTANTS);

            for (ChunkRenderList list : iterable(renderLists)) {
                RenderRegion region   = list.getRegion();
                ByteIterator iterator = list.sectionsWithGeometryIterator(false);

                if (iterator == null)
                    continue;

                while (iterator.hasNext()) {
                    int    sectionIndex = iterator.nextByteAsInt();
                    int    chunkX       = region.getChunkX() + LocalSectionIndex.unpackX(sectionIndex);
                    int    chunkY       = region.getChunkY() + LocalSectionIndex.unpackY(sectionIndex);
                    int    chunkZ       = region.getChunkZ() + LocalSectionIndex.unpackZ(sectionIndex);
                    long[] vertices     = drawnVertices(region, sectionIndex, camera, chunkX, chunkY, chunkZ);
                    double minX         = (chunkX << 4) - camera.x;
                    double minY         = (chunkY << 4) - camera.y;
                    double minZ         = (chunkZ << 4) - camera.z;

                    if (isNear(minX) && isNear(minY) && isNear(minZ)) {
                        untestedSections ++;

                        for (int index = 0; index < PASSES; index ++)
                            untestedVertices[index] += vertices[index];

                        continue;
                    }

                    constants.clear();
                    constants.putFloat((float) minX).putFloat((float) minY).putFloat((float) minZ).putFloat(0.0F).flip();
                    pass.pushConstants(constants);
                    VK10.vkCmdBeginQuery(commandBuffer, queryPool, queries, 0);
                    pass.draw(BOX_VERTICES, 1, 0, 0);
                    VK10.vkCmdEndQuery(commandBuffer, queryPool, queries);
                    sectionVertices[queries ++] = vertices;
                }
            }
        }
    }

    private static Iterable<ChunkRenderList> iterable(ChunkRenderListIterable renderLists) {
        return () -> renderLists.iterator(false);
    }

    private static boolean isNear(double min) {
        return min <= NEAR_MARGIN && min + SECTION_SIZE >= -NEAR_MARGIN;
    }

    private static long[] drawnVertices(RenderRegion region, int sectionIndex, CameraTransform camera, int chunkX, int chunkY, int chunkZ) {
        long[] vertices     = new long[PASSES];
        int    visibleFaces = DefaultChunkRenderer.getVisibleFaces(camera.intX, camera.intY, camera.intZ, chunkX, chunkY, chunkZ);

        for (TerrainRenderPass terrainPass : DefaultTerrainRenderPasses.ALL) {
            SectionRenderDataStorage storage = region.getStorage(terrainPass);

            if (storage == null)
                continue;

            long    pMeshData = storage.getDataPointer(sectionIndex);
            int     slices    = visibleFaces & SectionRenderDataUnsafe.getSliceMask(pMeshData);
            long    facings   = SectionRenderDataUnsafe.getFacingList(pMeshData);
            boolean local     = SectionRenderDataUnsafe.isLocalIndex(pMeshData);
            int     passIndex = DefaultTerrainRenderPasses.getPassIndex(terrainPass);

            for (int group = 0; group < ModelQuadFacing.COUNT; group ++) {
                int facing = local ? group : (int) (facings >>> group * 8 & 0xFFL);

                if ((slices >>> facing & 1) == 1)
                    vertices[passIndex] += SectionRenderDataUnsafe.getVertexCount(pMeshData, group);
            }
        }

        return vertices;
    }

    private static long createQueryPool(int count) {
        if (count == 0)
            throw new IllegalStateException("No terrain sections to probe");

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkQueryPoolCreateInfo info = VkQueryPoolCreateInfo.calloc(stack).sType$Default().queryType(VK10.VK_QUERY_TYPE_OCCLUSION).queryCount(count);
            LongBuffer            pool = stack.mallocLong(1);

            check(VK10.vkCreateQueryPool(device, info, null, pool), "create the occlusion query pool");
            VK12.vkResetQueryPool(device, pool.get(0), 0, count);

            return pool.get(0);
        }
    }

    private static void report() {
        LongBuffer samples = MemoryUtil.memAllocLong(queries);

        try {
            check(VK10.vkGetQueryPoolResults(device, queryPool, 0, queries, samples, Long.BYTES, VK10.VK_QUERY_RESULT_64_BIT | VK10.VK_QUERY_RESULT_WAIT_BIT), "read the occlusion queries");
            write(samples);
        } finally {
            MemoryUtil.memFree(samples);
            VK10.vkDestroyQueryPool(device, queryPool, null);
            queryPool = VK10.VK_NULL_HANDLE;
        }
    }

    private static void write(LongBuffer samples) {
        long[] total          = untestedVertices.clone();
        long[] hidden         = new long[PASSES];
        long   allTotal       = 0L;
        long   allHidden      = 0L;
        int    hiddenSections = 0;
        int    sections       = queries + untestedSections;

        for (int query = 0; query < queries; query ++) {
            boolean occluded = samples.get(query) == 0L;

            if (occluded)
                hiddenSections ++;

            for (int index = 0; index < PASSES; index ++) {
                total[index] += sectionVertices[query][index];

                if (occluded)
                    hidden[index] += sectionVertices[query][index];
            }
        }

        for (int index = 0; index < PASSES; index ++) {
            allTotal  += total[index];
            allHidden += hidden[index];
        }

        StringBuilder report = new StringBuilder();

        report.append(String.format(Locale.ROOT, "%d terrain sections drawn, %d hidden behind terrain (%.1f%%), %d next to the camera not tested%n", sections, hiddenSections, hiddenSections * 100.0 / sections, untestedSections));
        report.append(String.format(Locale.ROOT, "%d terrain vertices drawn, %d in hidden sections (%.1f%%)%n", allTotal, allHidden, allHidden * 100.0 / allTotal));

        for (TerrainRenderPass terrainPass : DefaultTerrainRenderPasses.ALL) {
            int index = DefaultTerrainRenderPasses.getPassIndex(terrainPass);

            report.append(String.format(Locale.ROOT, "%s: %d vertices, %.1f%% hidden%n", terrainPass.getPipeline().getLocation().getPath(), total[index], total[index] == 0L ? 0.0 : hidden[index] * 100.0 / total[index]));
        }

        Path file = Minecraft.getInstance().gameDirectory.toPath().resolve("logs").resolve("txocclusion-" + LocalDateTime.now().format(FILE_TIME) + ".txt");

        try {
            Files.writeString(file, report.toString());
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not write the occlusion probe to " + file, exception);
        }

        Component message = Component.literal("Occlusion probe saved to logs/" + file.getFileName() + "\n" + report.toString().strip());

        Minecraft.getInstance().schedule(() -> Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(message));
    }

    private static void check(int result, String action) {
        if (result != VK10.VK_SUCCESS)
            throw new IllegalStateException("Vulkan failed to " + action + ": " + result);
    }
}
