package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4f;

public record FrameModelSubmission(Function<ChunkSectionLayer, RenderType> renderTypes, boolean translucent, List<BlockStateModelPart> parts, Mesh mesh, int[] tints, int blockLightCoords) {

    private static final int SUBMIT_ARGUMENTS = 9;
    private static final int FIRST_LIGHT      = LightCoordsUtil.pack(3, 11);
    private static final int SECOND_LIGHT     = LightCoordsUtil.pack(14, 2);
    private static final int FIRST_OVERLAY    = 0x000A0003;
    private static final int SECOND_OVERLAY   = 0x00050007;
    private static final int FIRST_OUTLINE    = 0xFF123456;
    private static final int SECOND_OUTLINE   = 0;

    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords, int outlineColor) {
        collector.submitBlockModel(poseStack, this.renderTypes, this.translucent, this.parts, this.mesh, this.tints, LightCoordsUtil.max(lightCoords, this.blockLightCoords), overlayCoords, outlineColor);
    }

    public static FrameModelSubmission capture(BlockModelRenderState state) {
        Call first  = record(state, FIRST_LIGHT, FIRST_OVERLAY, FIRST_OUTLINE);
        Call second = record(state, SECOND_LIGHT, SECOND_OVERLAY, SECOND_OUTLINE);

        if (first == null || second == null || !first.sameAs(second))
            return null;

        if (!first.passes(state, FIRST_LIGHT, FIRST_OVERLAY, FIRST_OUTLINE) || !second.passes(state, SECOND_LIGHT, SECOND_OVERLAY, SECOND_OUTLINE))
            return null;

        return new FrameModelSubmission(first.renderTypes, first.translucent, first.parts, first.mesh, first.tints, state.blockLightCoords);
    }

    private static Call record(BlockModelRenderState state, int lightCoords, int overlayCoords, int outlineColor) {
        PoseStack poseStack = new PoseStack();
        Recorder  recorder  = new Recorder(poseStack);

        state.submitWithZOffset(poseStack, (SubmitNodeCollector) Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(), new Class<?>[] {SubmitNodeCollector.class}, recorder), lightCoords, overlayCoords, outlineColor);

        return recorder.single();
    }

    private record Call(Function<ChunkSectionLayer, RenderType> renderTypes, boolean translucent, List<BlockStateModelPart> parts, Mesh mesh, int[] tints, int lightCoords, int overlayCoords, int outlineColor) {

        private boolean sameAs(Call other) {
            if (this.translucent != other.translucent || !this.parts.equals(other.parts) || !Arrays.equals(this.tints, other.tints))
                return false;

            for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                if (this.renderTypes.apply(layer) != other.renderTypes.apply(layer))
                    return false;
            }

            return true;
        }

        private boolean passes(BlockModelRenderState state, int lightCoords, int overlayCoords, int outlineColor) {
            return this.lightCoords == LightCoordsUtil.max(lightCoords, state.blockLightCoords) && this.overlayCoords == overlayCoords && this.outlineColor == outlineColor;
        }
    }

    private static final class Recorder implements InvocationHandler {

        private final PoseStack poseStack;
        private final Matrix4f  pose;
        private Call            call;
        private int             calls;

        private Recorder(PoseStack poseStack) {
            this.poseStack = poseStack;
            this.pose      = new Matrix4f(poseStack.last().pose());
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object invoke(Object proxy, Method method, Object[] args) {
            this.calls ++;

            if (!method.getName().equals("submitBlockModel") || args == null || args.length != SUBMIT_ARGUMENTS || args[0] != this.poseStack || !this.poseStack.last().pose().equals(this.pose))
                return null;

            if (args[1] == null || args[4] == null)
                return null;

            this.call = new Call((Function<ChunkSectionLayer, RenderType>) args[1], (Boolean) args[2], (List<BlockStateModelPart>) args[3], (Mesh) args[4], (int[]) args[5], (Integer) args[6], (Integer) args[7], (Integer) args[8]);

            return null;
        }

        private Call single() {
            if (this.calls != 1)
                return null;

            return this.call;
        }
    }
}
