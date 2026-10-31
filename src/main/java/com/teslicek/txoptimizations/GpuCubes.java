package com.teslicek.txoptimizations;

import com.mojang.blaze3d.pipeline.PipelineCache;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.teslicek.txoptimizations.mixin.PipelineCacheAccessor;
import com.teslicek.txoptimizations.mixin.RenderSystemPipelineCacheAccessor;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

public final class GpuCubes {

    public static final int CUBE_VERTICES = CubeVertices.VERTICES;
    public static final int CUBE_INDICES  = CubeVertices.FACES * 6;
    public static final int POSE_BYTES    = 128;
    public static final int REF_BYTES     = 8;

    private static final String          DEFINE          = "TX_GPU_CUBES";
    private static final Identifier      SHADER          = Identifier.withDefaultNamespace("core/entity");
    private static final String          FILE            = "shaders/core/entity.vsh";
    private static final String          EXTENSION       = "#extension GL_ARB_separate_shader_objects : require\n";
    private static final String          EXTENSIONS      = EXTENSION + "#ifdef " + DEFINE + "\n#extension GL_ARB_gpu_shader5 : require\n#endif\n";
    private static final String          INPUTS          = "layout(location = 0) in vec3 Position;\nlayout(location = 1) in vec4 Color;\nlayout(location = 2) in vec2 UV0;\nlayout(location = 3) in ivec2 UV1;\nlayout(location = 4) in ivec2 UV2;\nlayout(location = 5) in vec3 Normal;\n";
    private static final String          MAIN            = "void main() {\n";
    private static final String          LOAD            = MAIN + "#ifdef " + DEFINE + "\n    txLoadCube();\n#endif\n";
    private static final String          MARKER          = "txLoadCube";
    private static final int             STATIC_FLOATS   = 56;
    private static final int             STATIC_BYTES    = STATIC_FLOATS * Float.BYTES;
    private static final int             INITIAL_CUBES   = 4096;
    private static final int             RETIRE_FRAMES   = 4;
    private static final BindGroupLayout LAYOUT          = BindGroupLayout.builder()
        .withUniform("TxCubeStatic", UniformType.TEXEL_BUFFER, GpuFormat.RGBA32_FLOAT)
        .withUniform("TxPoseMatrix", UniformType.TEXEL_BUFFER, GpuFormat.RGBA32_FLOAT)
        .withUniform("TxPoseInfo", UniformType.TEXEL_BUFFER, GpuFormat.RGBA32_UINT)
        .withUniform("TxPoseCoords", UniformType.TEXEL_BUFFER, GpuFormat.RG16_SINT)
        .withUniform("TxPoseColor", UniformType.TEXEL_BUFFER, GpuFormat.RGBA8_UNORM)
        .withUniform("TxPoseNormal", UniformType.TEXEL_BUFFER, GpuFormat.RGBA8_SNORM)
        .withUniform("TxCubeRefs", UniformType.TEXEL_BUFFER, GpuFormat.RG32_UINT)
        .build();

    private static final Map<RenderPipeline, RenderPipeline> VARIANTS    = new IdentityHashMap<>();
    private static final Map<RenderPipeline, Boolean>        ELIGIBLE    = new IdentityHashMap<>();
    private static final List<GpuBuffer>                     RETIRED     = new ArrayList<>();
    private static final List<Long>                          RETIRED_AT  = new ArrayList<>();
    private static final StagedVertexBuffer.ExecuteInfo      EMPTY_INFO  = new StagedVertexBuffer.ExecuteInfo(null, null, IndexType.SHORT, 0, 0, 0, PrimitiveTopology.QUADS);

    private static float[]                 statics = new float[STATIC_FLOATS * INITIAL_CUBES];
    private static int                     cubes;
    private static int                     uploadedCubes;
    private static @Nullable GpuBuffer     staticBuffer;
    private static int                     staticCapacity;
    private static int                     generation;
    private static PoseStack.@Nullable Pose partPose;
    private static int                     partLight;
    private static int                     partOverlay;
    private static int                     partColor;
    private static int                     partSlot;
    private static boolean                 reloaded;
    private static @Nullable GpuCubeDraw   pending;
    private static @Nullable PipelineCache checkedCache;
    private static boolean                 patchedSource;

    private GpuCubes() {
    }

    public static String patch(Identifier location, String source) {
        if (!location.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) || !location.getPath().equals(FILE))
            return source;

        if (!source.contains(EXTENSION) || !source.contains(INPUTS) || !source.contains(MAIN))
            return source;

        return source.replace(EXTENSION, EXTENSIONS).replace(INPUTS, "#ifdef " + DEFINE + "\n" + loader() + "#else\n" + INPUTS + "#endif\n").replace(MAIN, LOAD);
    }

    public static int generation() {
        return generation;
    }

    public static int register(float[] values) {
        if (values.length > STATIC_FLOATS)
            throw new IllegalArgumentException("A flattened cube has " + values.length + " values, more than the " + STATIC_FLOATS + " a GPU cube holds");

        if ((cubes + 1) * STATIC_FLOATS > statics.length)
            statics = Arrays.copyOf(statics, statics.length * 2);

        System.arraycopy(values, 0, statics, cubes * STATIC_FLOATS, values.length);

        return cubes ++;
    }

    public static @Nullable GpuCubeDraw beginPart(PoseStack.Pose pose, VertexConsumer builder, int light, int overlay, int color) {
        if (!NormalCache.usable() || builder.getClass() != BufferBuilder.class)
            return null;

        GpuCubeDraw draw = ((GpuCubeBuilder) builder).txoptimizations$cubeDraw();

        if (draw == null || !draw.txoptimizations$acceptsCubes())
            return null;

        partPose    = pose;
        partLight   = light;
        partOverlay = overlay;
        partColor   = color;
        partSlot    = -1;

        return draw;
    }

    public static boolean add(GpuCubeDraw draw, GpuCube cube, VertexConsumer builder) {
        int shape = cube.txoptimizations$gpuIndex();

        if (shape < 0)
            return false;

        GpuCubeBatch batch = draw.txoptimizations$batch();

        if (partSlot < 0) {
            PoseStack.Pose pose = partPose;

            if (pose == null)
                throw new IllegalStateException("A GPU cube was added outside a model part");

            partSlot = batch.pose(pose, partLight, partOverlay, partColor);
        }

        draw.txoptimizations$addCube(batch.addRef(shape, partSlot), draw.txoptimizations$rawVertices() + ((DirectVertexBuffer) builder).txoptimizations$vertexCount());

        return true;
    }

    public static void endPart() {
        partPose = null;
    }

    public static boolean accepts(VertexFormat format, PrimitiveTopology topology, @Nullable VertexSorting sorting, @Nullable PreparedRenderType prepared) {
        refreshShaders();

        if (prepared == null || format != DefaultVertexFormat.ENTITY || topology != PrimitiveTopology.QUADS || sorting != null || !isEligible(prepared.pipeline()))
            return false;

        OitPipelineSet oit = prepared.oitPipelineSet();

        if (oit == null)
            return true;

        for (OitStage stage : OitStage.values()) {
            if (!isEligible(oit.getPipeline(stage)))
                return false;
        }

        return true;
    }

    public static RenderPipeline variant(RenderPipeline vanilla) {
        if (!isEligible(vanilla))
            throw new IllegalStateException(vanilla.getLocation() + " draws GPU cubes but cannot compute them in its vertex shader");

        return VARIANTS.get(vanilla);
    }

    public static void uploadStatic(GpuDevice device) {
        if (uploadedCubes == cubes && staticBuffer != null)
            return;

        if (staticBuffer == null || cubes > staticCapacity) {
            int capacity = Math.max(INITIAL_CUBES, Integer.highestOneBit(Math.max(1, cubes - 1)) << 1);

            if (staticBuffer != null)
                retire(staticBuffer);

            staticBuffer   = device.createBuffer(() -> "TxOptimizations GPU cube shapes", GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER | GpuBuffer.USAGE_MAP_WRITE, (long) capacity * STATIC_BYTES);
            staticCapacity = capacity;
            uploadedCubes  = 0;
        }

        if (uploadedCubes == cubes)
            return;

        long offset = (long) uploadedCubes * STATIC_BYTES;
        long length = (long) (cubes - uploadedCubes) * STATIC_BYTES;

        try (GpuBufferSlice.MappedView view = staticBuffer.slice(offset, length).map(false, true)) {
            view.data().order(ByteOrder.nativeOrder()).asFloatBuffer().put(statics, uploadedCubes * STATIC_FLOATS, (cubes - uploadedCubes) * STATIC_FLOATS);
        }

        uploadedCubes = cubes;
    }

    public static void endFrame() {
        long now = ClientClock.frame();

        if (reloaded) {
            reloaded      = false;
            cubes         = 0;
            uploadedCubes = 0;
            generation ++;
        }

        for (int index = RETIRED.size() - 1; index >= 0; index --) {
            if (now - RETIRED_AT.get(index) < RETIRE_FRAMES)
                continue;

            RETIRED.remove(index).close();
            RETIRED_AT.remove(index);
        }
    }

    public static void retire(GpuBuffer buffer) {
        RETIRED.add(buffer);
        RETIRED_AT.add(ClientClock.frame());
    }

    public static StagedVertexBuffer.@Nullable ExecuteInfo executeInfo(StagedVertexBuffer staged, StagedVertexBuffer.Draw draw) {
        GpuCubeDraw cubes = (GpuCubeDraw) draw;

        if (pending != null)
            throw new IllegalStateException("A draw with GPU cubes was not drawn before the next draw");

        if (!cubes.txoptimizations$finishSegments())
            return staged.getExecuteInfo(draw);

        pending = cubes;

        if (draw.isEmpty())
            return EMPTY_INFO;

        return staged.getExecuteInfo(draw);
    }

    public static @Nullable GpuCubeDraw takePending() {
        GpuCubeDraw draw = pending;

        pending = null;

        return draw;
    }

    public static void bindCubes(RenderPass renderPass, GpuCubeBatch batch) {
        uploadStatic(RenderSystem.getDevice());
        renderPass.setUniform("TxCubeStatic", staticBuffer.slice(0L, (long) staticCapacity * STATIC_BYTES));
        batch.bind(renderPass);
    }

    public static void drawCubes(RenderPass renderPass, int firstCube, int cubeCount) {
        RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        int                                 count   = cubeCount * CUBE_INDICES;

        renderPass.setIndexBuffer(indices.getBuffer(count), indices.type());
        renderPass.drawIndexed(count, 1, 0, firstCube * CUBE_VERTICES, 0);
    }

    public static void requestIndices(int cubeCount) {
        if (cubeCount > 0)
            RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).requestIndexCount(cubeCount * CUBE_INDICES);
    }

    public static void writePose(long address, PoseStack.Pose pose, int light, int overlay, int color) {
        Matrix4f matrix = pose.pose();

        MemoryUtil.memPutFloat(address, matrix.m00());
        MemoryUtil.memPutFloat(address + 4L, matrix.m01());
        MemoryUtil.memPutFloat(address + 8L, matrix.m02());
        MemoryUtil.memPutFloat(address + 12L, matrix.m03());
        MemoryUtil.memPutFloat(address + 16L, matrix.m10());
        MemoryUtil.memPutFloat(address + 20L, matrix.m11());
        MemoryUtil.memPutFloat(address + 24L, matrix.m12());
        MemoryUtil.memPutFloat(address + 28L, matrix.m13());
        MemoryUtil.memPutFloat(address + 32L, matrix.m20());
        MemoryUtil.memPutFloat(address + 36L, matrix.m21());
        MemoryUtil.memPutFloat(address + 40L, matrix.m22());
        MemoryUtil.memPutFloat(address + 44L, matrix.m23());
        MemoryUtil.memPutFloat(address + 48L, matrix.m30());
        MemoryUtil.memPutFloat(address + 52L, matrix.m31());
        MemoryUtil.memPutFloat(address + 56L, matrix.m32());
        MemoryUtil.memPutFloat(address + 60L, matrix.m33());
        MemoryUtil.memPutInt(address + 64L, matrix.properties());
        MemoryUtil.memPutLong(address + 68L, VertexBytes.pairBits(overlay, light));
        MemoryUtil.memPutInt(address + 76L, VertexBytes.colorBits(color));
        NormalCache.selectAll(pose);

        for (int direction = 0; direction < CubeVertices.FACES; direction ++)
            VertexBytes.putPaddedNormal(address + 80L + direction * 4L, NormalCache.selected(direction));
    }

    private static boolean isEligible(@Nullable RenderPipeline pipeline) {
        return pipeline != null && ELIGIBLE.computeIfAbsent(pipeline, GpuCubes::eligible);
    }

    private static boolean eligible(RenderPipeline vanilla) {
        if (!patchedSource || !SHADER.equals(vanilla.getShaders().get(ShaderType.VERTEX)) || vanilla.getPrimitiveTopology() != PrimitiveTopology.QUADS || vanilla.getVertexFormatBinding(0) != DefaultVertexFormat.ENTITY)
            return false;

        List<VertexFormat> bindings = vanilla.getVertexFormatBindings();

        for (int slot = 1; slot < bindings.size(); slot ++) {
            if (bindings.get(slot) != null)
                return false;
        }

        VARIANTS.put(vanilla, variantOf(vanilla, bindings.size()));

        return true;
    }

    private static RenderPipeline variantOf(RenderPipeline vanilla, int slots) {
        ColorTargetState[]    targets = vanilla.getColorTargetStates().toArray(new ColorTargetState[0]);
        List<BindGroupLayout> layouts = new ArrayList<>(vanilla.getBindGroupLayouts());

        layouts.add(LAYOUT);

        RenderPipeline.Snippet copy = new RenderPipeline.Snippet(vanilla.getShaders(), Optional.of(vanilla.getShaderDefines()), Optional.of(layouts), targets, targets.length, Optional.ofNullable(vanilla.getDepthStencilState()), Optional.of(vanilla.getPolygonMode()), Optional.of(vanilla.isCull()), new VertexFormat[slots], Optional.of(vanilla.getPrimitiveTopology()), vanilla.pushConstantSize());

        return RenderPipeline.builder(copy).withLocation(Identifier.fromNamespaceAndPath("txoptimizations", vanilla.getLocation().getPath() + "_gpu_cubes")).withShaderDefine(DEFINE).build();
    }

    private static void refreshShaders() {
        PipelineCache cache = RenderSystemPipelineCacheAccessor.txoptimizations$currentPipelineCache();

        if (cache == null)
            throw new IllegalStateException("Entity models are drawn before any shaders were loaded");

        if (cache == checkedCache)
            return;

        String source = ((PipelineCacheAccessor) cache).txoptimizations$shaderSource().getShader(SHADER, ShaderType.VERTEX);

        reloaded      = reloaded || checkedCache != null;
        checkedCache  = cache;
        patchedSource = source != null && source.contains(MARKER);
        ELIGIBLE.clear();
        VARIANTS.clear();
    }

    private static String loader() {
        return "uniform samplerBuffer TxCubeStatic;\n"
            + "uniform samplerBuffer TxPoseMatrix;\n"
            + "uniform usamplerBuffer TxPoseInfo;\n"
            + "uniform isamplerBuffer TxPoseCoords;\n"
            + "uniform samplerBuffer TxPoseColor;\n"
            + "uniform samplerBuffer TxPoseNormal;\n"
            + "uniform usamplerBuffer TxCubeRefs;\n"
            + "const int TX_PLAIN[24] = int[](5, 4, 0, 1, 3, 2, 6, 7, 0, 4, 6, 2, 1, 0, 2, 3, 5, 1, 3, 7, 4, 5, 7, 6);\n"
            + "const int TX_MIRRORED[24] = int[](1, 0, 4, 5, 7, 6, 2, 3, 2, 6, 4, 0, 3, 2, 0, 1, 7, 3, 1, 5, 6, 7, 5, 4);\n"
            + "const int TX_PLAIN_FACING[6] = int[](0, 1, 4, 2, 5, 3);\n"
            + "const int TX_MIRRORED_FACING[6] = int[](0, 1, 5, 2, 4, 3);\n"
            + "vec3 Position;\n"
            + "vec4 Color;\n"
            + "vec2 UV0;\n"
            + "ivec2 UV1;\n"
            + "ivec2 UV2;\n"
            + "vec3 Normal;\n"
            + "float txShape(int cube, int value) {\n"
            + "    return texelFetch(TxCubeStatic, cube * 14 + (value >> 2))[value & 3];\n"
            + "}\n"
            + "void txLoadCube() {\n"
            + "    int cubeVertex = gl_VertexIndex / 24;\n"
            + "    int slot = gl_VertexIndex - cubeVertex * 24;\n"
            + "    uvec2 ref = texelFetch(TxCubeRefs, cubeVertex).xy;\n"
            + "    int cube = int(ref.x);\n"
            + "    int pose = int(ref.y);\n"
            + "    bool mirrored = txShape(cube, 0) != 0.0;\n"
            + "    int corner = mirrored ? TX_MIRRORED[slot] : TX_PLAIN[slot];\n"
            + "    float x = txShape(cube, (corner & 1) == 0 ? 1 : 4);\n"
            + "    float y = txShape(cube, (corner & 2) == 0 ? 2 : 5);\n"
            + "    float z = txShape(cube, (corner & 4) == 0 ? 3 : 6);\n"
            + "    uint properties = texelFetch(TxPoseInfo, pose * 8 + 4).x;\n"
            + "    vec4 c0 = texelFetch(TxPoseMatrix, pose * 8);\n"
            + "    vec4 c1 = texelFetch(TxPoseMatrix, pose * 8 + 1);\n"
            + "    vec4 c2 = texelFetch(TxPoseMatrix, pose * 8 + 2);\n"
            + "    vec4 c3 = texelFetch(TxPoseMatrix, pose * 8 + 3);\n"
            + "    precise vec3 position;\n"
            + "    if ((properties & " + Matrix4fc.PROPERTY_IDENTITY + "u) != 0u) {\n"
            + "        position = vec3(x, y, z);\n"
            + "    } else if ((properties & " + Matrix4fc.PROPERTY_TRANSLATION + "u) != 0u) {\n"
            + "        position = vec3(x + c3.x, y + c3.y, z + c3.z);\n"
            + "    } else {\n"
            + "        position = vec3(fma(c0.x, x, fma(c1.x, y, fma(c2.x, z, c3.x))), fma(c0.y, x, fma(c1.y, y, fma(c2.y, z, c3.y))), fma(c0.z, x, fma(c1.z, y, fma(c2.z, z, c3.z))));\n"
            + "    }\n"
            + "    Position = position;\n"
            + "    UV0 = vec2(txShape(cube, 7 + slot * 2), txShape(cube, 8 + slot * 2));\n"
            + "    UV1 = texelFetch(TxPoseCoords, pose * 32 + 17).xy;\n"
            + "    UV2 = texelFetch(TxPoseCoords, pose * 32 + 18).xy;\n"
            + "    Color = texelFetch(TxPoseColor, pose * 32 + 19);\n"
            + "    Normal = texelFetch(TxPoseNormal, pose * 32 + 20 + (mirrored ? TX_MIRRORED_FACING[slot >> 2] : TX_PLAIN_FACING[slot >> 2])).xyz;\n"
            + "}\n";
    }
}
