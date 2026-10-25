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
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class PackedTerrain {

    private static final String     DEFINE         = "TX_PACKED_TERRAIN";
    private static final String     VISIBLE        = "TX_SECTIONS_VISIBLE";
    private static final Identifier SHADER         = Identifier.withDefaultNamespace("core/terrain");
    private static final String     FILE           = "shaders/core/terrain.vsh";
    private static final String     FRAGMENT_FILE  = "shaders/core/terrain.fsh";
    private static final String     FADE           = "    chunkVisibility = mix(1.0, ChunkVisibility, clamp((dist - chunkFullyVisibleRange) / chunkFullyVisibleRange, 0.0, 1.0));\n";
    private static final String     FADE_OR_NONE   = "#ifdef " + VISIBLE + "\n    chunkVisibility = 1.0;\n#else\n" + FADE + "#endif\n";
    private static final String     FADE_FOG       = "    color = mix(FogColor * vec4(1, 1, 1, color.a), color, chunkVisibility);\n";
    private static final String     FADE_FOG_SKIP  = "#ifndef " + VISIBLE + "\n" + FADE_FOG + "#endif\n";
    private static final String     INPUTS         = "layout(location = 0) in vec3 Position;\nlayout(location = 1) in vec4 Color;\nlayout(location = 2) in vec2 UV0;\nlayout(location = 3) in ivec2 UV2;\n#ifdef MULTIDRAW_TERRAIN\nlayout(location = 4) in ivec3 ChunkPosition;\nlayout(location = 5) in float ChunkVisibility;\n#endif\n";
    private static final String     PACKED         = "#ifdef " + DEFINE + "\nlayout(location = 0) in uvec2 PositionData;\nlayout(location = 1) in vec4 Color;\nlayout(location = 2) in uvec2 UVLight;\n#define Position (vec3(uvec3(PositionData.x & 0xFFFFFu, PositionData.x >> 20u | (PositionData.y & 0xFFu) << 12u, PositionData.y >> 8u & 0xFFFFFu)) * (1.0 / 32768.0) - 8.0)\n#define UV0 (vec2(UVLight & 0xFFFFFFu) * (1.0 / 8388608.0))\n#define UV2 ivec2(UVLight >> 24u)\n#ifdef MULTIDRAW_TERRAIN\nlayout(location = 3) in ivec4 ChunkData;\n#define ChunkPosition ChunkData.xyz\n#define ChunkVisibility intBitsToFloat(ChunkData.w)\n#endif\n#else\n" + INPUTS + "#endif\n";
    private static final int        SLOTS          = 16;
    private static final int        BLOCK_ELEMENTS = 4;
    private static final int        CHUNK_ELEMENTS = 2;

    private static final Map<RenderPipeline, RenderPipeline> COMPACT         = new IdentityHashMap<>();
    private static final Map<RenderPipeline, RenderPipeline> VISIBLE_COMPACT = new IdentityHashMap<>();

    private static @Nullable PipelineCache checkedCache;
    private static boolean                 packedSource;
    private static boolean                 sectionsFading = true;

    private PackedTerrain() {
    }

    public static String patch(Identifier location, String source) {
        if (!location.getNamespace().equals(Identifier.DEFAULT_NAMESPACE))
            return source;

        if (location.getPath().equals(FRAGMENT_FILE))
            return source.replace(FADE_FOG, FADE_FOG_SKIP);

        if (!location.getPath().equals(FILE) || !source.contains(INPUTS))
            return source;

        if (!source.contains(FADE))
            throw new IllegalStateException("The vanilla terrain vertex shader no longer computes the section fade as " + FADE.strip());

        return source.replace(INPUTS, PACKED).replace(FADE, FADE_OR_NONE);
    }

    public static RenderPipeline terrainPipeline(RenderPipeline vanilla) {
        if (!CompactTerrain.active())
            return vanilla;

        if (sectionsFading)
            return COMPACT.computeIfAbsent(vanilla, pipeline -> compact(pipeline, false));

        return VISIBLE_COMPACT.computeIfAbsent(vanilla, pipeline -> compact(pipeline, true));
    }

    public static void setSectionsFading(boolean fading) {
        sectionsFading = fading;
    }

    public static boolean isPackedSource() {
        PipelineCache cache = RenderSystemPipelineCacheAccessor.txoptimizations$currentPipelineCache();

        if (cache == null)
            throw new IllegalStateException("Terrain is used before any shaders were loaded");

        if (cache != checkedCache) {
            String source = ((PipelineCacheAccessor) cache).txoptimizations$shaderSource().getShader(SHADER, ShaderType.VERTEX);

            checkedCache = cache;
            packedSource = source != null && source.contains(PACKED);
        }

        return packedSource;
    }

    private static RenderPipeline compact(RenderPipeline vanilla, boolean visible) {
        Identifier         shader  = vanilla.getShaders().get(ShaderType.VERTEX);
        VertexFormat       block   = vanilla.getVertexFormatBinding(0);
        VertexFormat       chunk   = vanilla.getVertexFormatBinding(1);
        VertexFormat[]     formats = new VertexFormat[SLOTS];
        ColorTargetState[] targets = vanilla.getColorTargetStates().toArray(new ColorTargetState[0]);

        if (!SHADER.equals(shader))
            throw new IllegalStateException(vanilla.getLocation() + " draws terrain with the vertex shader " + shader + " instead of " + SHADER);

        if (block == null || block.getElements().size() != BLOCK_ELEMENTS || chunk != null && chunk.getElements().size() != CHUNK_ELEMENTS || vanilla.getVertexFormatBindings().size() != SLOTS)
            throw new IllegalStateException(vanilla.getLocation() + " does not bind the block vertex format and at most the chunk section format " + vanilla.getVertexFormatBindings());

        if (block.getStepRate() != CompactTerrain.FORMAT.getStepRate())
            throw new IllegalStateException(vanilla.getLocation() + " steps its block vertices at rate " + block.getStepRate() + " instead of " + CompactTerrain.FORMAT.getStepRate());

        formats[0] = CompactTerrain.FORMAT;

        if (chunk != null) {
            formats[1] = VertexFormat.builder(chunk.getStepRate()).addAttribute("ChunkData", GpuFormat.RGBA32_SINT).build();
            requireLayout(chunk, formats[1], "ChunkPosition", GpuFormat.RGB32_SINT, 0);
            requireLayout(chunk, formats[1], "ChunkVisibility", GpuFormat.R32_FLOAT, 12);
        }

        for (int slot = 2; slot < SLOTS; slot ++) {
            if (vanilla.getVertexFormatBinding(slot) != null)
                throw new IllegalStateException(vanilla.getLocation() + " binds an unexpected vertex format in slot " + slot);
        }

        RenderPipeline.Snippet copy = new RenderPipeline.Snippet(vanilla.getShaders(), Optional.of(vanilla.getShaderDefines()), Optional.of(vanilla.getBindGroupLayouts()), targets, targets.length, Optional.ofNullable(vanilla.getDepthStencilState()), Optional.of(vanilla.getPolygonMode()), Optional.of(vanilla.isCull()), formats, Optional.of(vanilla.getPrimitiveTopology()), vanilla.pushConstantSize());

        RenderPipeline.Builder builder = RenderPipeline.builder(copy).withLocation(Identifier.fromNamespaceAndPath("txoptimizations", vanilla.getLocation().getPath() + (visible ? "_packed_visible" : "_packed"))).withShaderDefine(DEFINE);

        return (visible ? builder.withShaderDefine(VISIBLE) : builder).build();
    }

    private static void requireLayout(VertexFormat vanilla, VertexFormat packed, String name, GpuFormat format, int offset) {
        VertexFormatElement element = vanilla.getElement(name);

        if (vanilla.getVertexSize() != packed.getVertexSize() || vanilla.getStepRate() != packed.getStepRate())
            throw new IllegalStateException("Packed terrain format " + packed + " does not match the layout of " + vanilla);

        if (element == null || element.format() != format || element.offset() != offset)
            throw new IllegalStateException("Packed terrain reads " + name + " as " + format + " at byte " + offset + " but " + vanilla + " has " + element);
    }
}
