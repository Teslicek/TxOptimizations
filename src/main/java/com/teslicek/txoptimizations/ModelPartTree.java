package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public interface ModelPartTree {

    void txoptimizations$render(PoseStack.Pose parent, ModelPoseTree tree, VertexConsumer buffer, int lightCoords, int overlayCoords, int color);
}
