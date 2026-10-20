package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
    private static final int BOX_VERTICES = GizmoLineList.BOX_LINES * (LINE_VERTICES + 1) - 1;

    @Unique
    private static final int BOX_CORNERS = 8;

    @Unique
    private static final int CORNER_VALUES = 4;

    @Unique
    private final float[] txoptimizations$corners = new float[BOX_CORNERS * CORNER_VALUES];

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
        boolean        direct  = VertexBytes.LITTLE_ENDIAN && builder instanceof DirectVertexBuffer buffer && buffer.txoptimizations$duplicatesVertices() && buffer.txoptimizations$format() == DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH;

        if (direct)
            VertexBytes.requireLineLayout(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH);

        for (int entry = 0; entry < packed.entryCount(); entry ++) {
            int   lineColor = packed.color(entry);
            float lineWidth = packed.width(entry);

            if (!packed.isBox(entry)) {
                float startX = (float) (packed.coordinate(entry, 0) - camX);
                float startY = (float) (packed.coordinate(entry, 1) - camY);
                float startZ = (float) (packed.coordinate(entry, 2) - camZ);
                float endX   = (float) (packed.coordinate(entry, 3) - camX);
                float endY   = (float) (packed.coordinate(entry, 4) - camY);
                float endZ   = (float) (packed.coordinate(entry, 5) - camZ);

                txoptimizations$emitLine(builder, direct, startX, startY, startZ, txoptimizations$depth(viewX, viewY, viewZ, viewW, startX, startY, startZ), endX, endY, endZ, txoptimizations$depth(viewX, viewY, viewZ, viewW, endX, endY, endZ), lineColor, lineWidth);
                continue;
            }

            float x0 = (float) (packed.coordinate(entry, 0) - camX);
            float y0 = (float) (packed.coordinate(entry, 1) - camY);
            float z0 = (float) (packed.coordinate(entry, 2) - camZ);
            float x1 = (float) (packed.coordinate(entry, 3) - camX);
            float y1 = (float) (packed.coordinate(entry, 4) - camY);
            float z1 = (float) (packed.coordinate(entry, 5) - camZ);

            if (direct && txoptimizations$boxInFront(viewX, viewY, viewZ, viewW, x0, y0, z0, x1, y1, z1)) {
                VertexBytes.putLineBox(((DirectVertexBuffer) builder).txoptimizations$reserveAfterLastVertex(BOX_VERTICES), x0, y0, z0, x1, y1, z1, VertexBytes.colorBits(lineColor), VertexBytes.colorBits(packed.edgeColor(entry, 0)), VertexBytes.colorBits(packed.edgeColor(entry, 1)), VertexBytes.colorBits(packed.edgeColor(entry, 2)), lineWidth);
                continue;
            }

            float[] corners = this.txoptimizations$corners;

            for (int corner = 0; corner < BOX_CORNERS; corner ++) {
                float x    = (corner & 1) == 0 ? x0 : x1;
                float y    = (corner & 2) == 0 ? y0 : y1;
                float z    = (corner & 4) == 0 ? z0 : z1;
                int   base = corner * CORNER_VALUES;

                corners[base]     = x;
                corners[base + 1] = y;
                corners[base + 2] = z;
                corners[base + 3] = txoptimizations$depth(viewX, viewY, viewZ, viewW, x, y, z);
            }

            for (int edge = 0; edge < GizmoLineList.BOX_LINES; edge ++) {
                int from = GizmoLineList.edgeFrom(edge) * CORNER_VALUES;
                int to   = GizmoLineList.edgeTo(edge) * CORNER_VALUES;

                txoptimizations$emitLine(builder, direct, corners[from], corners[from + 1], corners[from + 2], corners[from + 3], corners[to], corners[to + 1], corners[to + 2], corners[to + 3], packed.edgeColor(entry, edge), lineWidth);
            }
        }
    }

    @Unique
    private static float txoptimizations$depth(float viewX, float viewY, float viewZ, float viewW, float x, float y, float z) {
        return Math.fma(viewX, x, Math.fma(viewY, y, Math.fma(viewZ, z, viewW * 1.0F)));
    }

    @Unique
    private static boolean txoptimizations$boxInFront(float viewX, float viewY, float viewZ, float viewW, float x0, float y0, float z0, float x1, float y1, float z1) {
        if (!Float.isFinite(x0) || !Float.isFinite(y0) || !Float.isFinite(z0) || !Float.isFinite(x1) || !Float.isFinite(y1) || !Float.isFinite(z1))
            return false;

        return txoptimizations$inFront(viewX, viewY, viewZ, viewW, x0, y0, z0) && txoptimizations$inFront(viewX, viewY, viewZ, viewW, x1, y0, z0) && txoptimizations$inFront(viewX, viewY, viewZ, viewW, x0, y1, z0) && txoptimizations$inFront(viewX, viewY, viewZ, viewW, x1, y1, z0)
            && txoptimizations$inFront(viewX, viewY, viewZ, viewW, x0, y0, z1) && txoptimizations$inFront(viewX, viewY, viewZ, viewW, x1, y0, z1) && txoptimizations$inFront(viewX, viewY, viewZ, viewW, x0, y1, z1) && txoptimizations$inFront(viewX, viewY, viewZ, viewW, x1, y1, z1);
    }

    @Unique
    private static boolean txoptimizations$inFront(float viewX, float viewY, float viewZ, float viewW, float x, float y, float z) {
        return !(txoptimizations$depth(viewX, viewY, viewZ, viewW, x, y, z) > NEAR_VIEW_Z);
    }

    @Unique
    private static void txoptimizations$emitLine(VertexConsumer builder, boolean direct, float startX, float startY, float startZ, float startDepth, float endX, float endY, float endZ, float endDepth, int lineColor, float lineWidth) {
        boolean startBehind = startDepth > NEAR_VIEW_Z;
        boolean endBehind   = endDepth > NEAR_VIEW_Z;

        if (startBehind && endBehind)
            return;

        if (startBehind || endBehind) {
            float denominator = endDepth - startDepth;

            if (Math.abs(denominator) < MIN_DENOMINATOR)
                return;

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
            return;
        }

        VertexBytes.putLineSegment(((DirectVertexBuffer) builder).txoptimizations$reserveAfterLastVertex(LINE_VERTICES), startX, startY, startZ, endX, endY, endZ, VertexBytes.colorBits(lineColor), VertexBytes.lineTail(VertexBytes.packNormal(normalX, normalY, normalZ), lineWidth));
    }
}
