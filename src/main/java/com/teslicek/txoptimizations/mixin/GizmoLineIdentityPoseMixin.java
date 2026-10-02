package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.teslicek.txoptimizations.DirectVertexBuffer;
import java.nio.ByteOrder;
import java.util.List;
import net.minecraft.client.renderer.feature.GizmoFeatureRenderer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives.Line;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Math;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GizmoFeatureRenderer.class)
public abstract class GizmoLineIdentityPoseMixin extends RenderTypeFeatureRenderer<GizmoFeatureRenderer.Submit> {

    @Unique
    private static final float NEAR_VIEW_Z = -0.05F;

    @Unique
    private static final float MIN_DENOMINATOR = 1.0E-9F;

    @Unique
    private static final int LINE_VERTICES = 3;

    @Unique
    private static final String POSITION = "Position";

    @Unique
    private static final String COLOR = "Color";

    @Unique
    private static final String NORMAL = "Normal";

    @Unique
    private static final String LINE_WIDTH = "LineWidth";

    @Unique
    private static final boolean LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    @Shadow
    @Final
    private PoseStack poseStack;

    @Overwrite
    private void buildLines(List<Line> lines, CameraRenderState camera, Matrix4fc modelViewMatrix, boolean opaque) {
        if (lines.isEmpty())
            return;

        if ((this.poseStack.last().pose().properties() & Matrix4fc.PROPERTY_IDENTITY) == 0)
            throw new IllegalStateException("Gizmo line pose is not the identity: " + this.poseStack.last().pose());

        VertexConsumer builder = this.getVertexBuilder(opaque ? RenderTypes.lines() : RenderTypes.linesTranslucentNoDepthWrite());
        double         camX    = camera.pos.x();
        double         camY    = camera.pos.y();
        double         camZ    = camera.pos.z();
        float          viewX   = modelViewMatrix.m02();
        float          viewY   = modelViewMatrix.m12();
        float          viewZ   = modelViewMatrix.m22();
        float          viewW   = modelViewMatrix.m32();
        VertexFormat   format  = builder instanceof DirectVertexBuffer buffer && buffer.txoptimizations$duplicatesVertices() ? buffer.txoptimizations$format() : null;
        boolean        direct  = format != null && format.contains(POSITION) && format.contains(COLOR) && format.contains(NORMAL) && format.contains(LINE_WIDTH);
        int            stride  = direct ? format.getVertexSize() : 0;
        int            color   = direct ? format.getElement(COLOR).offset() : 0;
        int            normal  = direct ? format.getElement(NORMAL).offset() : 0;
        int            width   = direct ? format.getElement(LINE_WIDTH).offset() : 0;
        int            origin  = direct ? format.getElement(POSITION).offset() : 0;

        for (Line line : lines) {
            float startX     = (float) (line.start().x() - camX);
            float startY     = (float) (line.start().y() - camY);
            float startZ     = (float) (line.start().z() - camZ);
            float endX       = (float) (line.end().x() - camX);
            float endY       = (float) (line.end().y() - camY);
            float endZ       = (float) (line.end().z() - camZ);
            float startDepth = Math.fma(viewX, startX, Math.fma(viewY, startY, Math.fma(viewZ, startZ, viewW * 1.0F)));
            float endDepth   = Math.fma(viewX, endX, Math.fma(viewY, endY, Math.fma(viewZ, endZ, viewW * 1.0F)));

            boolean startBehind = startDepth > NEAR_VIEW_Z;
            boolean endBehind   = endDepth > NEAR_VIEW_Z;

            if (startBehind && endBehind)
                continue;

            if (startBehind || endBehind) {
                float denominator = endDepth - startDepth;

                if (Math.abs(denominator) < MIN_DENOMINATOR)
                    continue;

                float t     = Mth.clamp((NEAR_VIEW_Z - startDepth) / denominator, 0.0F, 1.0F);
                float clipX = Math.fma(endX - startX, t, startX);
                float clipY = Math.fma(endY - startY, t, startY);
                float clipZ = Math.fma(endZ - startZ, t, startZ);

                if (startBehind) {
                    startX = clipX;
                    startY = clipY;
                    startZ = clipZ;
                } else {
                    endX = clipX;
                    endY = clipY;
                    endZ = clipZ;
                }
            }

            float normalX = endX - startX;
            float normalY = endY - startY;
            float normalZ = endZ - startZ;

            if (!direct) {
                builder.addVertex(startX, startY, startZ).setNormal(normalX, normalY, normalZ).setColor(line.color()).setLineWidth(line.width());
                builder.addVertex(endX, endY, endZ).setNormal(normalX, normalY, normalZ).setColor(line.color()).setLineWidth(line.width());
                continue;
            }

            long pointer = ((DirectVertexBuffer) builder).txoptimizations$reserveAfterLastVertex(LINE_VERTICES);
            int  abgr    = ARGB.toABGR(line.color());

            txoptimizations$putVertex(pointer, origin, color, normal, width, startX, startY, startZ, abgr, normalX, normalY, normalZ, line.width());
            MemoryUtil.memCopy(pointer, pointer + stride, stride);
            txoptimizations$putVertex(pointer + 2L * stride, origin, color, normal, width, endX, endY, endZ, abgr, normalX, normalY, normalZ, line.width());
        }
    }

    @Unique
    private static void txoptimizations$putVertex(long pointer, int origin, int color, int normal, int width, float x, float y, float z, int abgr, float normalX, float normalY, float normalZ, float lineWidth) {
        MemoryUtil.memPutFloat(pointer + origin, x);
        MemoryUtil.memPutFloat(pointer + origin + 4L, y);
        MemoryUtil.memPutFloat(pointer + origin + 8L, z);
        MemoryUtil.memPutInt(pointer + color, LITTLE_ENDIAN ? abgr : Integer.reverseBytes(abgr));
        MemoryUtil.memPutByte(pointer + normal, txoptimizations$normalByte(normalX));
        MemoryUtil.memPutByte(pointer + normal + 1L, txoptimizations$normalByte(normalY));
        MemoryUtil.memPutByte(pointer + normal + 2L, txoptimizations$normalByte(normalZ));
        MemoryUtil.memPutFloat(pointer + width, lineWidth);
    }

    @Unique
    private static byte txoptimizations$normalByte(float value) {
        return (byte) ((int) (Mth.clamp(value, -1.0F, 1.0F) * 127.0F) & 0xFF);
    }
}
