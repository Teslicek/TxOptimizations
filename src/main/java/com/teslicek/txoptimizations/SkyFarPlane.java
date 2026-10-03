package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.dimension.DimensionType;

public final class SkyFarPlane {

    private static final String            DEFINE         = "TX_FAR_PLANE";
    private static final String            POSITION       = "gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);";
    private static final Set<String>       SHADERS        = Set.of("shaders/core/sky.vsh", "shaders/core/stars.vsh", "shaders/core/position_color.vsh", "shaders/core/position_tex.vsh");
    private static final Set<String>       PATCHED        = ConcurrentHashMap.newKeySet();
    private static final DepthStencilState BEHIND_TERRAIN = new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false);

    public static final RenderPipeline SKY = RenderPipeline.builder()
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withBindGroupLayout(BindGroupLayouts.PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
        .withBindGroupLayout(BindGroupLayouts.FOG)
        .withLocation(Identifier.fromNamespaceAndPath("txoptimizations", "pipeline/sky_far"))
        .withVertexShader("core/sky")
        .withFragmentShader("core/sky")
        .withShaderDefine(DEFINE)
        .withVertexBinding(0, DefaultVertexFormat.POSITION)
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_FAN)
        .withColorTargetState(ColorTargetState.DEFAULT)
        .withDepthStencilState(BEHIND_TERRAIN)
        .build();

    public static final RenderPipeline SUNRISE_SUNSET = RenderPipeline.builder()
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withBindGroupLayout(BindGroupLayouts.PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
        .withLocation(Identifier.fromNamespaceAndPath("txoptimizations", "pipeline/sunrise_sunset_far"))
        .withVertexShader("core/position_color")
        .withFragmentShader("core/position_color")
        .withShaderDefine(DEFINE)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_FAN)
        .withDepthStencilState(BEHIND_TERRAIN)
        .build();

    public static final RenderPipeline STARS = RenderPipeline.builder()
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withBindGroupLayout(BindGroupLayouts.PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
        .withLocation(Identifier.fromNamespaceAndPath("txoptimizations", "pipeline/stars_far"))
        .withVertexShader("core/stars")
        .withFragmentShader("core/stars")
        .withShaderDefine(DEFINE)
        .withColorTargetState(new ColorTargetState(BlendFunction.OVERLAY))
        .withVertexBinding(0, DefaultVertexFormat.POSITION)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withDepthStencilState(BEHIND_TERRAIN)
        .build();

    public static final RenderPipeline CELESTIAL = RenderPipeline.builder()
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withBindGroupLayout(BindGroupLayouts.PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
        .withLocation(Identifier.fromNamespaceAndPath("txoptimizations", "pipeline/celestial_far"))
        .withVertexShader("core/position_tex")
        .withFragmentShader("core/position_tex")
        .withShaderDefine(DEFINE)
        .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
        .withColorTargetState(new ColorTargetState(BlendFunction.OVERLAY))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withDepthStencilState(BEHIND_TERRAIN)
        .build();

    private static SkyRenderer    deferredRenderer;
    private static GpuBufferSlice deferredFog;
    private static SkyRenderState deferredState;
    private static boolean        drawing;

    private SkyFarPlane() {
    }

    public static String patch(Identifier location, String source) {
        if (!location.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) || !SHADERS.contains(location.getPath()))
            return source;

        if (!source.contains(POSITION)) {
            PATCHED.remove(location.getPath());

            return source;
        }

        PATCHED.add(location.getPath());

        return source.replace(POSITION, POSITION + "\n#ifdef " + DEFINE + "\n    gl_Position.z = 0.0;\n#endif");
    }

    public static boolean canDefer(SkyRenderState state, boolean improvedTransparency) {
        return state.skybox == DimensionType.Skybox.OVERWORLD && !improvedTransparency && PATCHED.size() == SHADERS.size();
    }

    public static void defer(SkyRenderer renderer, GpuBufferSlice fog, SkyRenderState state) {
        if (deferredState != null)
            throw new IllegalStateException("The previous frame's sky was never drawn");

        deferredRenderer = renderer;
        deferredFog      = fog;
        deferredState    = state;
    }

    public static boolean hasDeferred() {
        return deferredState != null;
    }

    public static void drawDeferred() {
        SkyRenderer    renderer = deferredRenderer;
        GpuBufferSlice fog      = deferredFog;
        SkyRenderState state    = deferredState;

        deferredRenderer = null;
        deferredFog      = null;
        deferredState    = null;
        drawing          = true;

        renderer.render(fog, state);

        drawing = false;
    }

    public static boolean isDrawing() {
        return drawing;
    }
}
