package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.teslicek.txoptimizations.GpuCube;
import com.teslicek.txoptimizations.GpuCubeBatch;
import com.teslicek.txoptimizations.GpuCubeDraw;
import com.teslicek.txoptimizations.GpuCubes;
import com.teslicek.txoptimizations.ModelPartTree;
import com.teslicek.txoptimizations.ModelPoseTree;
import com.teslicek.txoptimizations.ModelRotation;
import com.teslicek.txoptimizations.SkinAlpha;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import org.jspecify.annotations.Nullable;
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

    @Unique
    private int @Nullable [] txoptimizations$shapes;

    @Unique
    private int txoptimizations$shapesGeneration = -1;

    @Unique
    private int @Nullable [] txoptimizations$shapes() {
        if (this.txoptimizations$shapesGeneration == GpuCubes.generation())
            return this.txoptimizations$shapes;

        int[] shapes = new int[this.cubes.size()];

        for (int index = 0; index < shapes.length; index ++) {
            shapes[index] = ((GpuCube) this.cubes.get(index)).txoptimizations$gpuIndex();

            if (shapes[index] < 0) {
                shapes = null;
                break;
            }
        }

        this.txoptimizations$shapes           = shapes;
        this.txoptimizations$shapesGeneration = GpuCubes.generation();

        return shapes;
    }

    @Overwrite
    public void render(PoseStack poseStack, VertexConsumer buffer, int lightCoords, int overlayCoords, int color) {
        this.txoptimizations$renderFrom(poseStack.last(), buffer, lightCoords, overlayCoords, color);
    }

    @Override
    public void txoptimizations$renderFrom(PoseStack.Pose pose, VertexConsumer buffer, int lightCoords, int overlayCoords, int color) {
        this.txoptimizations$renderArray(pose, ModelPoseTree.current(), buffer, lightCoords, overlayCoords, color);
    }

    @Unique
    private ModelPart @Nullable [] txoptimizations$treeParts;

    @Unique
    private int[] txoptimizations$treeEnds;

    @Unique
    private int[] txoptimizations$treeDepths;

    @Unique
    private int txoptimizations$treeHeight;

    @Unique
    private void txoptimizations$renderArray(PoseStack.Pose parent, ModelPoseTree tree, VertexConsumer buffer, int lightCoords, int overlayCoords, int color) {
        ModelPart[] parts = this.txoptimizations$treeParts;

        if (parts == null) {
            this.txoptimizations$buildTree();
            parts = this.txoptimizations$treeParts;
        }

        int[]            ends   = this.txoptimizations$treeEnds;
        int[]            depths = this.txoptimizations$treeDepths;
        PoseStack.Pose[] chain  = tree.chain(this.txoptimizations$treeHeight + 2);

        chain[0] = parent;

        GpuCubeDraw  draw     = GpuCubes.modelDraw(buffer);
        GpuCubeBatch batch    = draw == null ? null : draw.txoptimizations$batch();
        int          runFirst = -1;
        int          runCount = 0;
        int          runRaw   = 0;
        PoseStack.Pose lastPose = null;
        int            lastSlot = 0;

        for (int index = 0; index < parts.length; index ++) {
            ModelPartIdentityMixin part = (ModelPartIdentityMixin) (Object) parts[index];

            if (!part.visible) {
                index = ends[index] - 1;
                continue;
            }

            int            depth = depths[index];
            PoseStack.Pose above = chain[depth];
            boolean        same  = part.txoptimizations$isIdentity();
            PoseStack.Pose pose  = same ? above : part.txoptimizations$transform(tree.at(depth), above);

            chain[depth + 1] = pose;

            if (!same)
                lastPose = null;

            if (!part.txoptimizations$hasCubes || part.skipDraw)
                continue;

            int[] shapes = draw == null ? null : part.txoptimizations$shapes();

            if (shapes == null) {
                if (runCount > 0) {
                    draw.txoptimizations$addRefs(GpuCubeDraw.CUBES, runFirst, runCount, runRaw);
                    runCount = 0;
                }

                part.compile(pose, buffer, lightCoords, overlayCoords, color);
                lastPose = null;
                continue;
            }

            if (pose != lastPose) {
                lastSlot = batch.pose(pose, lightCoords, overlayCoords, color);
                lastPose = pose;
            }

            if (runCount == 0) {
                runRaw   = GpuCubes.rawVertices(draw, buffer);
                runFirst = batch.addRefs(shapes, lastSlot);
            } else {
                batch.addRefs(shapes, lastSlot);
            }

            runCount += shapes.length;
        }

        if (runCount > 0)
            draw.txoptimizations$addRefs(GpuCubeDraw.CUBES, runFirst, runCount, runRaw);
    }

    @Unique
    private void txoptimizations$buildTree() {
        List<ModelPart> parts  = new ArrayList<>();
        List<Integer>   depths = new ArrayList<>();

        txoptimizations$collect((ModelPart) (Object) this, 0, parts, depths);

        int   size = parts.size();
        int[] ends = new int[size];
        int[] flat = new int[size];
        int   high = 0;

        for (int index = 0; index < size; index ++) {
            int depth = depths.get(index);
            int end   = index + 1;

            while (end < size && depths.get(end) > depth)
                end ++;

            ends[index] = end;
            flat[index] = depth;
            high        = Math.max(high, depth);
        }

        this.txoptimizations$treeEnds   = ends;
        this.txoptimizations$treeDepths = flat;
        this.txoptimizations$treeHeight = high;
        this.txoptimizations$treeParts  = parts.toArray(ModelPart[]::new);
    }

    @Unique
    private static void txoptimizations$collect(ModelPart part, int depth, List<ModelPart> parts, List<Integer> depths) {
        ModelPartIdentityMixin self = (ModelPartIdentityMixin) (Object) part;

        if (self.cubes.isEmpty() && self.children.isEmpty())
            return;

        self.txoptimizations$hasCubes = !self.cubes.isEmpty();
        parts.add(part);
        depths.add(depth);

        for (ModelPart child : self.children.values())
            txoptimizations$collect(child, depth + 1, parts, depths);
    }

    @Unique
    private boolean txoptimizations$hasCubes;

    @Unique
    private float @Nullable [] txoptimizations$rotation;

    @Unique
    private int txoptimizations$rotationX;

    @Unique
    private int txoptimizations$rotationY;

    @Unique
    private int txoptimizations$rotationZ;

    @Unique
    private void txoptimizations$rotate(PoseStack.Pose pose, PoseStack.Pose parent) {
        int bitsX = Float.floatToRawIntBits(this.xRot);
        int bitsY = Float.floatToRawIntBits(this.yRot);
        int bitsZ = Float.floatToRawIntBits(this.zRot);

        float[] rotation = this.txoptimizations$rotation;

        if (rotation == null || bitsX != this.txoptimizations$rotationX || bitsY != this.txoptimizations$rotationY || bitsZ != this.txoptimizations$rotationZ) {
            if (rotation == null) {
                rotation                     = ModelRotation.newRotation();
                this.txoptimizations$rotation = rotation;
            }

            ModelRotation.fill(rotation, this.zRot, this.yRot, this.xRot);
            this.txoptimizations$rotationX = bitsX;
            this.txoptimizations$rotationY = bitsY;
            this.txoptimizations$rotationZ = bitsZ;
        }

        ModelRotation.translateRotate(pose, parent, this.x / 16.0F, this.y / 16.0F, this.z / 16.0F, rotation);
    }

    @Unique
    private PoseStack.Pose txoptimizations$transform(PoseStack.Pose pose, PoseStack.Pose parent) {
        if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F)
            this.txoptimizations$rotate(pose, parent);
        else
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
