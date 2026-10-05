package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.teslicek.txoptimizations.ArrayMapValues;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ModelPart.class)
public abstract class ModelPartChildrenMixin {

    @Shadow
    public boolean visible;

    @Shadow
    public boolean skipDraw;

    @Shadow
    @Final
    private List<ModelPart.Cube> cubes;

    @Shadow
    @Final
    public Map<String, ModelPart> children;

    @Shadow
    public abstract void translateAndRotate(PoseStack poseStack);

    @Shadow
    protected abstract void compile(PoseStack.Pose pose, VertexConsumer buffer, int lightCoords, int overlayCoords, int color);

    @Overwrite
    public void render(PoseStack poseStack, VertexConsumer buffer, int lightCoords, int overlayCoords, int color) {
        if (!this.visible)
            return;

        if (this.cubes.isEmpty() && this.children.isEmpty())
            return;

        poseStack.pushPose();
        this.translateAndRotate(poseStack);

        if (!this.skipDraw)
            this.compile(poseStack.last(), buffer, lightCoords, overlayCoords, color);

        if (this.children instanceof Object2ObjectArrayMap<String, ModelPart> arrayMap) {
            Object[] parts = ArrayMapValues.values(arrayMap);
            int      size  = ArrayMapValues.size(arrayMap);

            for (int index = 0; index < size; index ++)
                ((ModelPart) parts[index]).render(poseStack, buffer, lightCoords, overlayCoords, color);
        } else {
            for (ModelPart child : this.children.values())
                child.render(poseStack, buffer, lightCoords, overlayCoords, color);
        }

        poseStack.popPose();
    }
}
