package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.renderer.Octree;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.joml.FrustumIntersection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Octree.Branch.class, priority = 999)
public abstract class OctreeBranchSectionLeafMixin {

    @Shadow
    @Final
    private Octree.Node[] nodes;

    @Shadow
    @Final
    private BoundingBox boundingBox;

    @Shadow
    @Final
    private int bbCenterX;

    @Shadow
    @Final
    private int bbCenterY;

    @Shadow
    @Final
    private int bbCenterZ;

    @Shadow
    @Final
    private Octree.AxisSorting sorting;

    @Shadow
    @Final
    private boolean cameraXDiffNegative;

    @Shadow
    @Final
    private boolean cameraYDiffNegative;

    @Shadow
    @Final
    private boolean cameraZDiffNegative;

    @Shadow
    @Final
    private Octree this$0;

    @Shadow
    private static int getNodeIndex(Octree.AxisSorting sorting, boolean xDiffsOppositeSides, boolean yDiffsOppositeSides, boolean zDiffsOppositeSides) {
        throw new AssertionError();
    }

    @Shadow
    protected abstract boolean areChildrenLeaves();

    @Shadow
    protected abstract BoundingBox createChildBoundingBox(boolean sectionXDiffNegative, boolean sectionYDiffNegative, boolean sectionZDiffNegative);

    @Overwrite
    public boolean add(SectionRenderDispatcher.RenderSection section) {
        long    sectionNode          = section.getSectionNode();
        boolean sectionXDiffNegative = SectionPos.sectionToBlockCoord(SectionPos.x(sectionNode)) - this.bbCenterX < 0;
        boolean sectionYDiffNegative = SectionPos.sectionToBlockCoord(SectionPos.y(sectionNode)) - this.bbCenterY < 0;
        boolean sectionZDiffNegative = SectionPos.sectionToBlockCoord(SectionPos.z(sectionNode)) - this.bbCenterZ < 0;
        int     nodeIndex            = getNodeIndex(this.sorting, sectionXDiffNegative != this.cameraXDiffNegative, sectionYDiffNegative != this.cameraYDiffNegative, sectionZDiffNegative != this.cameraZDiffNegative);

        if (this.areChildrenLeaves()) {
            boolean alreadyExisted = this.nodes[nodeIndex] != null;

            this.nodes[nodeIndex] = (Octree.Node) (Object) section;

            return !alreadyExisted;
        }

        if (this.nodes[nodeIndex] != null)
            return ((Octree.Branch) this.nodes[nodeIndex]).add(section);

        Octree.Branch branch = this.this$0.new Branch(this.createChildBoundingBox(sectionXDiffNegative, sectionYDiffNegative, sectionZDiffNegative));

        this.nodes[nodeIndex] = branch;

        return branch.add(section);
    }

    @Overwrite
    public void visitNodes(Octree.OctreeVisitor visitor, boolean skipFrustumCheck, Frustum frustum, int depth, int closeDistance, boolean isClose) {
        boolean isVisible = skipFrustumCheck;

        if (!skipFrustumCheck) {
            int checkResult = frustum.cubeInFrustum(this.boundingBox);

            skipFrustumCheck = checkResult == FrustumIntersection.INSIDE;
            isVisible        = checkResult == FrustumIntersection.INSIDE || checkResult == FrustumIntersection.INTERSECT;
        }

        if (!isVisible)
            return;

        isClose = isClose && this.this$0.isClose(this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.minZ(), this.boundingBox.maxX(), this.boundingBox.maxY(), this.boundingBox.maxZ(), closeDistance);
        visitor.visit((Octree.Node) (Object) this, skipFrustumCheck, depth, isClose);

        for (Octree.Node node : this.nodes) {
            if (node instanceof SectionRenderDispatcher.RenderSection section)
                this.txoptimizations$visitSection(section, visitor, skipFrustumCheck, frustum, depth + 1, closeDistance, isClose);
            else if (node != null)
                node.visitNodes(visitor, skipFrustumCheck, frustum, depth + 1, closeDistance, isClose);
        }
    }

    @Unique
    private void txoptimizations$visitSection(SectionRenderDispatcher.RenderSection section, Octree.OctreeVisitor visitor, boolean skipFrustumCheck, Frustum frustum, int depth, int closeDistance, boolean isClose) {
        AABB box = section.getBoundingBox();

        if (!skipFrustumCheck && !frustum.isVisible(box))
            return;

        visitor.visit((Octree.Node) (Object) section, skipFrustumCheck, depth, isClose && this.this$0.isClose(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, closeDistance));
    }
}
