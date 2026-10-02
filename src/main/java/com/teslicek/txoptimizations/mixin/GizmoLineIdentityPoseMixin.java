package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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

@Mixin(GizmoFeatureRenderer.class)
public abstract class GizmoLineIdentityPoseMixin extends RenderTypeFeatureRenderer<GizmoFeatureRenderer.Submit> {

    @Unique
    private static final float NEAR_VIEW_Z = -0.05F;

    @Unique
    private static final float MIN_DENOMINATOR = 1.0E-9F;

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

            builder.addVertex(startX, startY, startZ).setNormal(normalX, normalY, normalZ).setColor(line.color()).setLineWidth(line.width());
            builder.addVertex(endX, endY, endZ).setNormal(normalX, normalY, normalZ).setColor(line.color()).setLineWidth(line.width());
        }
    }
}
