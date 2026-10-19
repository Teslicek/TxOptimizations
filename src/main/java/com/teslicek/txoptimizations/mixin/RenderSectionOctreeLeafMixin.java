package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.renderer.Octree;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class RenderSectionOctreeLeafMixin implements Octree.Node {

    @Shadow
    public abstract AABB getBoundingBox();

    @Override
    public void visitNodes(Octree.OctreeVisitor visitor, boolean skipFrustumCheck, Frustum frustum, int depth, int closeDistance, boolean isClose) {
        throw new UnsupportedOperationException("Octree leaf sections are visited by the branch that holds them");
    }

    @Override
    public SectionRenderDispatcher.RenderSection getSection() {
        return (SectionRenderDispatcher.RenderSection) (Object) this;
    }

    @Override
    public AABB getAABB() {
        return this.getBoundingBox();
    }
}
