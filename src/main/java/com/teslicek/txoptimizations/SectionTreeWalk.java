package com.teslicek.txoptimizations;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.client.renderer.Octree;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.joml.FrustumIntersection;

public final class SectionTreeWalk {

    private static final int CLOSE_DISTANCE   = 32;
    private static final int SECTION_SIZE     = 16;
    private static final int LEAF_PARENT_SPAN = 32;
    private static final int CAMERA_CUBE      = 8;

    private final Frustum                                      frustum;
    private final List<SectionRenderDispatcher.RenderSection> visible;
    private final List<Object>                                 visibleNodes;
    private final List<SectionRenderDispatcher.RenderSection> nearby;
    private final int                                          cameraX;
    private final int                                          cameraY;
    private final int                                          cameraZ;

    @SuppressWarnings("unchecked")
    private SectionTreeWalk(Frustum frustum, List<SectionRenderDispatcher.RenderSection> visible, List<SectionRenderDispatcher.RenderSection> nearby, BlockPos cameraSectionCenter) {
        this.frustum      = frustum;
        this.visible      = visible;
        this.visibleNodes = (List<Object>) (List<?>) visible;
        this.nearby       = nearby;
        this.cameraX      = cameraSectionCenter.getX();
        this.cameraY      = cameraSectionCenter.getY();
        this.cameraZ      = cameraSectionCenter.getZ();
    }

    private void branch(Octree.Branch branch, boolean inside, boolean close) {
        BoundingBox box = branch.boundingBox;

        if (!inside) {
            int result = this.frustum.cubeInFrustum(box);

            if (result != FrustumIntersection.INSIDE && result != FrustumIntersection.INTERSECT)
                return;

            inside = result == FrustumIntersection.INSIDE;
        }

        boolean       childrenClose = close && this.isClose(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
        Octree.Node[] nodes         = branch.nodes;

        if (box.getXSpan() != LEAF_PARENT_SPAN) {
            for (Octree.Node node : nodes)
                if (node != null)
                    this.branch((Octree.Branch) node, inside, childrenClose);

            return;
        }

        if (inside && !childrenClose) {
            for (Octree.Node node : nodes)
                if (node != null)
                    this.visibleNodes.add(node);

            return;
        }

        for (Octree.Node node : nodes)
            if (node != null)
                this.leaf((SectionRenderDispatcher.RenderSection) node, inside, childrenClose);
    }

    private void leaf(SectionRenderDispatcher.RenderSection section, boolean inside, boolean close) {
        long sectionNode = section.getSectionNode();
        int  minX        = SectionPos.sectionToBlockCoord(SectionPos.x(sectionNode));
        int  minY        = SectionPos.sectionToBlockCoord(SectionPos.y(sectionNode));
        int  minZ        = SectionPos.sectionToBlockCoord(SectionPos.z(sectionNode));

        if (!inside && !((FrustumBoxTest) this.frustum).txoptimizations$isBoxVisible(minX, minY, minZ, minX + SECTION_SIZE, minY + SECTION_SIZE, minZ + SECTION_SIZE))
            return;

        this.visible.add(section);

        if (close && this.isClose(minX, minY, minZ, minX + SECTION_SIZE, minY + SECTION_SIZE, minZ + SECTION_SIZE))
            this.nearby.add(section);
    }

    private boolean isClose(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.cameraX > minX - CLOSE_DISTANCE && this.cameraX < maxX + CLOSE_DISTANCE && this.cameraY > minY - CLOSE_DISTANCE && this.cameraY < maxY + CLOSE_DISTANCE && this.cameraZ > minZ - CLOSE_DISTANCE && this.cameraZ < maxZ + CLOSE_DISTANCE;
    }

    private void sortNearestFirst() {
        int size = this.visible.size();

        if (size < 2)
            return;

        if (!(this.visible instanceof ObjectArrayList<SectionRenderDispatcher.RenderSection> list))
            throw new IllegalStateException("Visible sections are kept in a " + this.visible.getClass().getName() + " instead of an ObjectArrayList");

        Object[] sections = list.elements();
        int[]    keys     = new int[size];
        int      cameraX  = SectionPos.blockToSectionCoord(this.cameraX);
        int      cameraY  = SectionPos.blockToSectionCoord(this.cameraY);
        int      cameraZ  = SectionPos.blockToSectionCoord(this.cameraZ);
        int      largest  = 0;

        for (int index = 0; index < size; index ++) {
            long node = ((SectionRenderDispatcher.RenderSection) sections[index]).getSectionNode();
            int  x    = SectionPos.x(node) - cameraX;
            int  y    = SectionPos.y(node) - cameraY;
            int  z    = SectionPos.z(node) - cameraZ;

            keys[index] = x * x + y * y + z * z;
            largest     = Math.max(largest, keys[index]);
        }

        int[] starts = new int[largest + 2];

        for (int key : keys)
            starts[key + 1] ++;

        for (int key = 1; key < starts.length; key ++)
            starts[key] += starts[key - 1];

        for (int index = 0; index < size; index ++)
            keys[index] = starts[keys[index]] ++;

        for (int index = 0; index < size; index ++) {
            for (int target = keys[index]; target != index; target = keys[index]) {
                Object section = sections[target];

                sections[target] = sections[index];
                sections[index]  = section;
                keys[index]      = keys[target];
                keys[target]     = target;
            }
        }
    }

    public static void collect(Octree octree, Frustum frustum, List<SectionRenderDispatcher.RenderSection> visible, List<SectionRenderDispatcher.RenderSection> nearby) {
        SectionTreeWalk walk = new SectionTreeWalk(new Frustum(frustum).offsetToFullyIncludeCameraCube(CAMERA_CUBE), visible, nearby, octree.cameraSectionCenter);

        walk.branch(octree.root, false, true);
        walk.sortNearestFirst();
    }
}
