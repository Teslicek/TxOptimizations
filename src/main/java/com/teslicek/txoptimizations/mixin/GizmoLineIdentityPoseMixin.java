package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.teslicek.txoptimizations.DirectVertexBuffer;
import com.teslicek.txoptimizations.GizmoLineList;
import com.teslicek.txoptimizations.VertexBytes;
import java.util.List;
import net.minecraft.client.renderer.feature.GizmoFeatureRenderer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives.Line;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import org.joml.Math;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = GizmoFeatureRenderer.class, priority = 999)
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

    @Shadow
    @Final
    private PoseStack poseStack;

    @Overwrite
    private void buildLines(List<Line> lines, CameraRenderState camera, Matrix4fc modelViewMatrix, boolean opaque) {
        if (lines.isEmpty())
            return;

        if (!(lines instanceof GizmoLineList packed))
            throw new IllegalStateException("Gizmo lines are not packed: " + lines.getClass().getName());

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

        for (int index = 0; index < packed.size(); index ++) {
            float startX     = (float) (packed.coordinate(index, 0) - camX);
            float startY     = (float) (packed.coordinate(index, 1) - camY);
            float startZ     = (float) (packed.coordinate(index, 2) - camZ);
            float endX       = (float) (packed.coordinate(index, 3) - camX);
            float endY       = (float) (packed.coordinate(index, 4) - camY);
            float endZ       = (float) (packed.coordinate(index, 5) - camZ);
            int   lineColor  = packed.color(index);
            float lineWidth  = packed.width(index);
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
                builder.addVertex(startX, startY, startZ).setNormal(normalX, normalY, normalZ).setColor(lineColor).setLineWidth(lineWidth);
                builder.addVertex(endX, endY, endZ).setNormal(normalX, normalY, normalZ).setColor(lineColor).setLineWidth(lineWidth);
                continue;
            }

            long pointer = ((DirectVertexBuffer) builder).txoptimizations$reserveAfterLastVertex(LINE_VERTICES);

            txoptimizations$putVertex(pointer, origin, color, normal, width, startX, startY, startZ, lineColor, normalX, normalY, normalZ, lineWidth);
            MemoryUtil.memCopy(pointer, pointer + stride, stride);
            txoptimizations$putVertex(pointer + 2L * stride, origin, color, normal, width, endX, endY, endZ, lineColor, normalX, normalY, normalZ, lineWidth);
        }
    }

    @Unique
    private static void txoptimizations$putVertex(long pointer, int origin, int color, int normal, int width, float x, float y, float z, int argb, float normalX, float normalY, float normalZ, float lineWidth) {
        VertexBytes.putPosition(pointer + origin, x, y, z);
        VertexBytes.putColor(pointer + color, argb);
        VertexBytes.putNormal(pointer + normal, VertexBytes.packNormal(normalX, normalY, normalZ));
        MemoryUtil.memPutFloat(pointer + width, lineWidth);
    }
}
