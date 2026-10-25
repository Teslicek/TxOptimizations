package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.teslicek.txoptimizations.GizmoPointList;
import java.util.List;
import net.minecraft.client.renderer.feature.GizmoFeatureRenderer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives.Point;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GizmoFeatureRenderer.class, priority = 999)
public abstract class GizmoPointsPackedMixin extends RenderTypeFeatureRenderer<GizmoFeatureRenderer.Submit> {

    @Shadow
    @Final
    private PoseStack poseStack;

    @Overwrite
    private void buildPoints(List<Point> points, CameraRenderState camera) {
        if (points.isEmpty())
            return;

        if (!(points instanceof GizmoPointList packed))
            throw new IllegalStateException("Gizmo points are not packed: " + points.getClass().getName());

        VertexConsumer builder = this.getVertexBuilder(RenderTypes.debugPoint());
        PoseStack.Pose pose    = this.poseStack.last();
        double         camX    = camera.pos.x();
        double         camY    = camera.pos.y();
        double         camZ    = camera.pos.z();

        for (int index = 0; index < packed.size(); index ++)
            builder.addVertex(pose, (float) (packed.coordinate(index, 0) - camX), (float) (packed.coordinate(index, 1) - camY), (float) (packed.coordinate(index, 2) - camZ)).setColor(packed.color(index)).setLineWidth(packed.pointSize(index));
    }
}
