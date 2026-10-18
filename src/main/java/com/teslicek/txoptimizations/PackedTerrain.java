package com.teslicek.txoptimizations;

import com.mojang.blaze3d.pipeline.PipelineCache;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import com.teslicek.txoptimizations.mixin.PipelineCacheAccessor;
import com.teslicek.txoptimizations.mixin.RenderSystemPipelineCacheAccessor;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class PackedTerrain {

    private static final String     DEFINE         = "TX_PACKED_TERRAIN";
    private static final Identifier SHADER         = Identifier.withDefaultNamespace("core/terrain");
    private static final String     FILE           = "shaders/core/terrain.vsh";
    private static final String     INPUTS         = "layout(location = 2) in vec2 UV0;\nlayout(location = 3) in ivec2 UV2;\n#ifdef MULTIDRAW_TERRAIN\nlayout(location = 4) in ivec3 ChunkPosition;\nlayout(location = 5) in float ChunkVisibility;\n#endif\n";
    private static final String     PACKED         = "#ifdef " + DEFINE + "\nlayout(location = 2) in uvec3 UVData;\nlayout(location = 3) in ivec4 ChunkData;\n#define UV0 uintBitsToFloat(UVData.xy)\n#define UV2 ivec2((int(UVData.z) << 16) >> 16, int(UVData.z) >> 16)\n#define ChunkPosition ChunkData.xyz\n#define ChunkVisibility intBitsToFloat(ChunkData.w)\n#else\n" + INPUTS + "#endif\n";
    private static final int        SLOTS          = 16;
    private static final int        BLOCK_ELEMENTS = 4;
    private static final int        CHUNK_ELEMENTS = 2;
    private static final boolean    IRIS           = FabricLoader.getInstance().isModLoaded("iris");

    private static @Nullable PipelineCache checkedCache;
    private static boolean                 packedSource;

    private PackedTerrain() {
    }

    public static String patch(Identifier location, String source) {
        if (!location.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) || !location.getPath().equals(FILE) || !source.contains(INPUTS))
            return source;

        return source.replace(INPUTS, PACKED);
    }

    public static RenderPipeline multiDrawPipeline(ChunkSectionLayer layer, RenderPipeline vanilla) {
        if (IRIS)
            return vanilla;

        PipelineCache cache = RenderSystemPipelineCacheAccessor.txoptimizations$currentPipelineCache();

        if (cache == null)
            throw new IllegalStateException("Terrain is drawn before any shaders were loaded");

        if (cache != checkedCache) {
            String source = ((PipelineCacheAccessor) cache).txoptimizations$shaderSource().getShader(SHADER, ShaderType.VERTEX);

            checkedCache = cache;
            packedSource = source != null && source.contains(PACKED);
        }

        if (!packedSource)
            return vanilla;

        return Pipelines.VALUES[layer.ordinal()];
    }

    private static RenderPipeline pack(ChunkSectionLayer layer) {
        RenderPipeline     vanilla = layer.pipeline(true);
        VertexFormat       block   = vanilla.getVertexFormatBinding(0);
        VertexFormat       chunk   = vanilla.getVertexFormatBinding(1);
        VertexFormat[]     formats = new VertexFormat[SLOTS];
        ColorTargetState[] targets = vanilla.getColorTargetStates().toArray(new ColorTargetState[0]);

        if (block == null || chunk == null || block.getElements().size() != BLOCK_ELEMENTS || chunk.getElements().size() != CHUNK_ELEMENTS || vanilla.getVertexFormatBindings().size() != SLOTS)
            throw new IllegalStateException(vanilla.getLocation() + " does not bind exactly the block and chunk section vertex formats " + vanilla.getVertexFormatBindings());

        formats[0] = VertexFormat.builder(block.getStepRate()).addAttribute("Position", GpuFormat.RGB32_FLOAT).addAttribute("Color", GpuFormat.RGBA8_UNORM).addAttribute("UVData", GpuFormat.RGB32_UINT).build();
        formats[1] = VertexFormat.builder(chunk.getStepRate()).addAttribute("ChunkData", GpuFormat.RGBA32_SINT).build();

        for (int slot = 2; slot < SLOTS; slot ++) {
            if (vanilla.getVertexFormatBinding(slot) != null)
                throw new IllegalStateException(vanilla.getLocation() + " binds an unexpected vertex format in slot " + slot);
        }

        requireLayout(block, formats[0], "Position", GpuFormat.RGB32_FLOAT, "Position", 0);
        requireLayout(block, formats[0], "Color", GpuFormat.RGBA8_UNORM, "Color", 0);
        requireLayout(block, formats[0], "UV0", GpuFormat.RG32_FLOAT, "UVData", 0);
        requireLayout(block, formats[0], "UV2", GpuFormat.RG16_SINT, "UVData", 8);
        requireLayout(chunk, formats[1], "ChunkPosition", GpuFormat.RGB32_SINT, "ChunkData", 0);
        requireLayout(chunk, formats[1], "ChunkVisibility", GpuFormat.R32_FLOAT, "ChunkData", 12);

        RenderPipeline.Snippet copy = new RenderPipeline.Snippet(vanilla.getShaders(), Optional.of(vanilla.getShaderDefines()), Optional.of(vanilla.getBindGroupLayouts()), targets, targets.length, Optional.ofNullable(vanilla.getDepthStencilState()), Optional.of(vanilla.getPolygonMode()), Optional.of(vanilla.isCull()), formats, Optional.of(vanilla.getPrimitiveTopology()), vanilla.pushConstantSize());

        return RenderPipeline.builder(copy).withLocation(Identifier.fromNamespaceAndPath("txoptimizations", vanilla.getLocation().getPath() + "_packed")).withShaderDefine(DEFINE).build();
    }

    private static void requireLayout(VertexFormat vanilla, VertexFormat packed, String name, GpuFormat format, String packedName, int offsetInPacked) {
        VertexFormatElement element = vanilla.getElement(name);

        if (vanilla.getVertexSize() != packed.getVertexSize() || vanilla.getStepRate() != packed.getStepRate())
            throw new IllegalStateException("Packed terrain format " + packed + " does not match the layout of " + vanilla);

        if (element == null || element.format() != format)
            throw new IllegalStateException("Packed terrain reads " + name + " as " + format + " but " + vanilla + " has " + element);

        int packedOffset = packed.getElement(packedName).offset() + offsetInPacked;

        if (element.offset() != packedOffset)
            throw new IllegalStateException(name + " is at byte " + element.offset() + " of " + vanilla + " but packed terrain reads it at byte " + packedOffset);
    }

    private static final class Pipelines {

        private static final RenderPipeline[] VALUES = new RenderPipeline[SectionLayers.VALUES.length];

        static {
            for (ChunkSectionLayer layer : SectionLayers.VALUES)
                VALUES[layer.ordinal()] = pack(layer);
        }
    }
}
