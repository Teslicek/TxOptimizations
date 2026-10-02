package com.teslicek.txoptimizations.cull;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryStack;

public final class DepthReadback {

    public static final int WIDTH  = 320;
    public static final int HEIGHT = 180;

    private static final int             TEXELS          = WIDTH * HEIGHT;
    private static final int             SLOTS           = 3;
    private static final int             PUSH_CONSTANTS  = 16;
    private static final int             TEXTURE_USAGE   = GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_SRC;
    private static final int             BUFFER_USAGE    = GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST;
    private static final int             WRITE_RED       = 1;
    private static final BindGroupLayout SAMPLER         = BindGroupLayout.builder().withUniform("DepthSampler", UniformType.COMBINED_IMAGE_SAMPLER).build();
    private static final RenderPipeline  PIPELINE        = RenderPipeline.builder()
        .withLocation(Identifier.fromNamespaceAndPath("txoptimizations", "depth_downsample"))
        .withBindGroupLayout(SAMPLER)
        .withPushConstantSize(PUSH_CONSTANTS)
        .withCull(false)
        .withVertexShader(Identifier.fromNamespaceAndPath("txoptimizations", "shaderc/depth_downsample"))
        .withFragmentShader(Identifier.fromNamespaceAndPath("txoptimizations", "shaderc/depth_downsample"))
        .withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.R32_FLOAT, WRITE_RED))
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .build();
    private static final Slot[]          RING            = new Slot[SLOTS];
    private static final float[]         FRAME_MATRIX    = new float[16];
    private static final float[]         LATEST_DEPTH    = new float[TEXELS];
    private static final float[]         LATEST_MATRIX   = new float[16];

    private static GpuTexture     target;
    private static GpuTextureView targetView;
    private static boolean        frameKnown;
    private static double         frameX;
    private static double         frameY;
    private static double         frameZ;
    private static long           generation;
    private static long           latestVersion;
    private static double         latestX;
    private static double         latestY;
    private static double         latestZ;

    private DepthReadback() {
    }

    public static void describeFrame(float[] viewProjection, double cameraX, double cameraY, double cameraZ) {
        System.arraycopy(viewProjection, 0, FRAME_MATRIX, 0, FRAME_MATRIX.length);
        frameX     = cameraX;
        frameY     = cameraY;
        frameZ     = cameraZ;
        frameKnown = true;
    }

    public static boolean canCapture() {
        return frameKnown && freeSlot() != null;
    }

    public static void capture(RenderTarget mainTarget) {
        Slot slot = freeSlot();

        if (!frameKnown || slot == null)
            throw new IllegalStateException("Depth capture requested without a described frame or a free slot");

        createTarget();

        GpuTexture depth = mainTarget.getDepthTexture();

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "TxOptimizations depth downsample", targetView, Optional.empty());
             MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer sizes = stack.malloc(PUSH_CONSTANTS);

            sizes.putInt(depth.getWidth(0)).putInt(depth.getHeight(0)).putInt(WIDTH).putInt(HEIGHT).flip();
            pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
            pass.setUniform("DepthSampler", mainTarget.getDepthTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            pass.pushConstants(sizes);
            pass.draw(3, 1, 0, 0);
        }

        long captureGeneration = generation;

        slot.pending = true;
        slot.x       = frameX;
        slot.y       = frameY;
        slot.z       = frameZ;
        System.arraycopy(FRAME_MATRIX, 0, slot.matrix, 0, FRAME_MATRIX.length);
        frameKnown = false;

        RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(target, slot.buffer, 0L, () -> complete(slot, captureGeneration), 0);
    }

    public static void reset() {
        generation ++;
        latestVersion = 0L;
        frameKnown    = false;
    }

    public static long latestVersion() {
        return latestVersion;
    }

    public static float[] latestDepth() {
        return LATEST_DEPTH;
    }

    public static float[] latestMatrix() {
        return LATEST_MATRIX;
    }

    public static double latestX() {
        return latestX;
    }

    public static double latestY() {
        return latestY;
    }

    public static double latestZ() {
        return latestZ;
    }

    private static void complete(Slot slot, long captureGeneration) {
        slot.pending = false;

        if (captureGeneration != generation)
            return;

        try (GpuBufferSlice.MappedView view = slot.buffer.map(true, false)) {
            view.data().order(ByteOrder.nativeOrder()).asFloatBuffer().get(0, LATEST_DEPTH);
        }

        System.arraycopy(slot.matrix, 0, LATEST_MATRIX, 0, LATEST_MATRIX.length);
        latestX = slot.x;
        latestY = slot.y;
        latestZ = slot.z;
        latestVersion ++;
    }

    private static Slot freeSlot() {
        for (int index = 0; index < SLOTS; index ++) {
            if (RING[index] == null)
                RING[index] = new Slot();

            if (!RING[index].pending)
                return RING[index];
        }

        return null;
    }

    private static void createTarget() {
        if (target != null)
            return;

        target     = RenderSystem.getDevice().createTexture(() -> "TxOptimizations occlusion depth", TEXTURE_USAGE, GpuFormat.R32_FLOAT, WIDTH, HEIGHT, 1, 1);
        targetView = RenderSystem.getDevice().createTextureView(target);
    }

    private static final class Slot {

        private final GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "TxOptimizations occlusion readback", BUFFER_USAGE, (long) TEXELS * Float.BYTES);
        private final float[]   matrix = new float[16];
        private boolean         pending;
        private double          x;
        private double          y;
        private double          z;
    }
}
