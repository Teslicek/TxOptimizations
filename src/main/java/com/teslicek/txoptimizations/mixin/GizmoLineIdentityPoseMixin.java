package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.feature.GizmoFeatureRenderer;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GizmoFeatureRenderer.class)
public abstract class GizmoLineIdentityPoseMixin {

    @Redirect(method = "buildLines", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer txoptimizations$addVertexWithoutTransform(VertexConsumer builder, PoseStack.Pose pose, float x, float y, float z) {
        txoptimizations$requireIdentity(pose);

        return builder.addVertex(x, y, z);
    }

    @Redirect(method = "buildLines", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setNormal(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer txoptimizations$setNormalWithoutTransform(VertexConsumer builder, PoseStack.Pose pose, float x, float y, float z) {
        return builder.setNormal(x, y, z);
    }

    @Unique
    private static void txoptimizations$requireIdentity(PoseStack.Pose pose) {
        if ((pose.pose().properties() & Matrix4fc.PROPERTY_IDENTITY) == 0)
            throw new IllegalStateException("Gizmo line pose is not the identity: " + pose.pose());
    }
}
