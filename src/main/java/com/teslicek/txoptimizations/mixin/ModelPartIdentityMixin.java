package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.teslicek.txoptimizations.ArrayMapValues;
import com.teslicek.txoptimizations.GpuCube;
import com.teslicek.txoptimizations.GpuCubeDraw;
import com.teslicek.txoptimizations.GpuCubes;
import com.teslicek.txoptimizations.ModelPartTree;
import com.teslicek.txoptimizations.ModelPoseTree;
import com.teslicek.txoptimizations.ModelRotation;
import com.teslicek.txoptimizations.SkinAlpha;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = ModelPart.class, priority = 999)
public abstract class ModelPartIdentityMixin implements ModelPartTree {

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

    @Overwrite
    public void translateAndRotate(PoseStack poseStack) {
        poseStack.translate(this.x / 16.0F, this.y / 16.0F, this.z / 16.0F);

        if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F)
            ModelRotation.rotateZYX(poseStack.last(), this.zRot, this.yRot, this.xRot);

        this.txoptimizations$scale(poseStack.last());
    }

    @Overwrite
    private void compile(PoseStack.Pose pose, VertexConsumer builder, int lightCoords, int overlayCoords, int color) {
        GpuCubeDraw draw = GpuCubes.beginPart(pose, builder, lightCoords, overlayCoords, color);
        SkinAlpha   skin = draw == null ? null : draw.txoptimizations$skin();

        for (int index = 0; index < this.cubes.size(); index ++) {
            ModelPart.Cube cube   = this.cubes.get(index);
            int            hidden = skin == null ? 0 : skin.hiddenFaces(cube);

            if (skin != null && hidden == SkinAlpha.allFaces(cube))
                continue;

            if (draw == null || !GpuCubes.add(draw, (GpuCube) cube, builder, hidden))
                cube.compile(pose, builder, lightCoords, overlayCoords, color);
        }

        if (draw != null)
            GpuCubes.endPart(draw);
    }

    @Overwrite
    public void render(PoseStack poseStack, VertexConsumer buffer, int lightCoords, int overlayCoords, int color) {
        this.txoptimizations$render(poseStack.last(), ModelPoseTree.current(), buffer, lightCoords, overlayCoords, color);
    }

    @Override
    public void txoptimizations$render(PoseStack.Pose parent, ModelPoseTree tree, VertexConsumer buffer, int lightCoords, int overlayCoords, int color) {
        if (!this.visible || this.cubes.isEmpty() && this.children.isEmpty())
            return;

        boolean        transformed = !this.txoptimizations$isIdentity();
        PoseStack.Pose pose        = transformed ? this.txoptimizations$transform(tree.push(), parent) : parent;

        if (!this.skipDraw && !this.cubes.isEmpty())
            this.compile(pose, buffer, lightCoords, overlayCoords, color);

        if (this.children instanceof Object2ObjectArrayMap<String, ModelPart> arrayMap) {
            Object[] parts = ArrayMapValues.values(arrayMap);
            int      size  = ArrayMapValues.size(arrayMap);

            for (int index = 0; index < size; index ++)
                ((ModelPartTree) parts[index]).txoptimizations$render(pose, tree, buffer, lightCoords, overlayCoords, color);
        } else {
            for (ModelPart child : this.children.values())
                ((ModelPartTree) (Object) child).txoptimizations$render(pose, tree, buffer, lightCoords, overlayCoords, color);
        }

        if (transformed)
            tree.pop();
    }

    @Unique
    private PoseStack.Pose txoptimizations$transform(PoseStack.Pose pose, PoseStack.Pose parent) {
        ModelRotation.translateRotate(pose, parent, this.x / 16.0F, this.y / 16.0F, this.z / 16.0F, this.zRot, this.yRot, this.xRot);
        this.txoptimizations$scale(pose);

        return pose;
    }

    @Unique
    private void txoptimizations$scale(PoseStack.Pose pose) {
        if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F)
            pose.scale(this.xScale, this.yScale, this.zScale);
    }

    @Unique
    private boolean txoptimizations$isIdentity() {
        return this.x == 0.0F && this.y == 0.0F && this.z == 0.0F && this.xRot == 0.0F && this.yRot == 0.0F && this.zRot == 0.0F && this.xScale == 1.0F && this.yScale == 1.0F && this.zScale == 1.0F;
    }
}
