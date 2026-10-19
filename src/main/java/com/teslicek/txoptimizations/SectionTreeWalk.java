package com.teslicek.txoptimizations;

import java.util.List;
import net.minecraft.client.renderer.Octree;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.joml.FrustumIntersection;

public final class SectionTreeWalk {

    private static final int CLOSE_DISTANCE = 32;

    private final Frustum                                      frustum;
    private final List<SectionRenderDispatcher.RenderSection> visible;
    private final List<SectionRenderDispatcher.RenderSection> nearby;
    private final int                                          cameraX;
    private final int                                          cameraY;
    private final int                                          cameraZ;

    private SectionTreeWalk(Frustum frustum, List<SectionRenderDispatcher.RenderSection> visible, List<SectionRenderDispatcher.RenderSection> nearby, BlockPos cameraSectionCenter) {
        this.frustum = frustum;
        this.visible = visible;
        this.nearby  = nearby;
        this.cameraX = cameraSectionCenter.getX();
        this.cameraY = cameraSectionCenter.getY();
        this.cameraZ = cameraSectionCenter.getZ();
    }

    private void branch(Octree.Branch branch, boolean inside, boolean close) {
        BoundingBox box = branch.boundingBox;

        if (!inside) {
            int result = this.frustum.cubeInFrustum(box);

            if (result != FrustumIntersection.INSIDE && result != FrustumIntersection.INTERSECT)
                return;

            inside = result == FrustumIntersection.INSIDE;
        }

        boolean childrenClose = close && this.isClose(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
        boolean untested      = inside && !childrenClose;

        for (Octree.Node node : branch.nodes) {
            if (node instanceof Octree.Branch child)
                this.branch(child, inside, childrenClose);
            else if (node != null && untested)
                this.visible.add(((Octree.Leaf) node).section);
            else if (node != null)
                this.leaf(((Octree.Leaf) node).section, inside, childrenClose);
        }
    }

    private void leaf(SectionRenderDispatcher.RenderSection section, boolean inside, boolean close) {
        AABB box = section.getBoundingBox();

        if (!inside && !this.frustum.isVisible(box))
            return;

        this.visible.add(section);

        if (close && this.isClose(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ))
            this.nearby.add(section);
    }

    private boolean isClose(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.cameraX > minX - CLOSE_DISTANCE && this.cameraX < maxX + CLOSE_DISTANCE && this.cameraY > minY - CLOSE_DISTANCE && this.cameraY < maxY + CLOSE_DISTANCE && this.cameraZ > minZ - CLOSE_DISTANCE && this.cameraZ < maxZ + CLOSE_DISTANCE;
    }

    public static void collect(Octree octree, Frustum frustum, List<SectionRenderDispatcher.RenderSection> visible, List<SectionRenderDispatcher.RenderSection> nearby) {
        new SectionTreeWalk(frustum, visible, nearby, octree.cameraSectionCenter).branch(octree.root, false, true);
    }
}
