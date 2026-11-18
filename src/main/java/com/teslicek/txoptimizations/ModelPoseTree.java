package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Arrays;

public final class ModelPoseTree {

    private static final int                        INITIAL_DEPTH = 16;
    private static final ModelPoseTree              RENDER        = new ModelPoseTree();
    private static final ThreadLocal<ModelPoseTree> WORKERS       = ThreadLocal.withInitial(ModelPoseTree::new);

    private PoseStack.Pose[] poses = new PoseStack.Pose[0];
    private int              depth;
    private PoseStack.Pose[] chain = new PoseStack.Pose[INITIAL_DEPTH];

    private ModelPoseTree() {
        this.grow(INITIAL_DEPTH);
    }

    public PoseStack.Pose[] chain(int length) {
        if (this.chain.length < length)
            this.chain = new PoseStack.Pose[length * 2];

        return this.chain;
    }

    public PoseStack.Pose at(int slot) {
        if (slot >= this.poses.length)
            this.grow(slot * 2);

        return this.poses[slot];
    }

    public PoseStack.Pose push() {
        if (this.depth == this.poses.length)
            this.grow(this.depth * 2);

        return this.poses[this.depth ++];
    }

    public void pop() {
        if (this.depth == 0)
            throw new IllegalStateException("A model part released more poses than it took");

        this.depth --;
    }

    private void grow(int length) {
        int filled = this.poses.length;

        this.poses = Arrays.copyOf(this.poses, length);

        for (int index = filled; index < length; index ++)
            this.poses[index] = new PoseStack.Pose();
    }

    public static ModelPoseTree current() {
        return RenderThreadStack.isOwner() ? RENDER : WORKERS.get();
    }
}
