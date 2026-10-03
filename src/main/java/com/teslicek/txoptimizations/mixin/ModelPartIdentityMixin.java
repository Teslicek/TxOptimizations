package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ModelPart.class)
public abstract class ModelPartIdentityMixin {

    @Shadow
    public float x;

    @Shadow
    public float y;

    @Shadow
    public float z;

    @Shadow
    public float xRot;

    @Shadow
    public float yRot;

    @Shadow
    public float zRot;

    @Shadow
    public float xScale;

    @Shadow
    public float yScale;

    @Shadow
    public float zScale;

    @Shadow
    public boolean visible;

    @Shadow
    public boolean skipDraw;

    @Shadow
    @Final
    private List<ModelPart.Cube> cubes;

    @Shadow
    @Final
    private Map<String, ModelPart> children;

    @Shadow
    public abstract void translateAndRotate(PoseStack poseStack);

    @Shadow
    private void compile(PoseStack.Pose pose, VertexConsumer builder, int lightCoords, int overlayCoords, int color) {
        throw new AssertionError();
    }

    @Overwrite
    public void render(PoseStack poseStack, VertexConsumer buffer, int lightCoords, int overlayCoords, int color) {
        if (!this.visible || this.cubes.isEmpty() && this.children.isEmpty())
            return;

        boolean transformed = !this.txoptimizations$isIdentity();

        if (transformed) {
            poseStack.pushPose();
            this.translateAndRotate(poseStack);
        }

        if (!this.skipDraw)
            this.compile(poseStack.last(), buffer, lightCoords, overlayCoords, color);

        for (ModelPart child : this.children.values())
            child.render(poseStack, buffer, lightCoords, overlayCoords, color);

        if (transformed)
            poseStack.popPose();
    }

    @Unique
    private boolean txoptimizations$isIdentity() {
        return this.x == 0.0F && this.y == 0.0F && this.z == 0.0F && this.xRot == 0.0F && this.yRot == 0.0F && this.zRot == 0.0F && this.xScale == 1.0F && this.yScale == 1.0F && this.zScale == 1.0F;
    }
}
