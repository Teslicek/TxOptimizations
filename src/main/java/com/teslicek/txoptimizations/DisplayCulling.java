package com.teslicek.txoptimizations;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.phys.AABB;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

public final class DisplayCulling {

    private static final Matrix4f            VIEW_PROJECTION = new Matrix4f();
    private static final FrustumIntersection FRUSTUM         = new FrustumIntersection();
    private static final Vector3f            CORNER          = new Vector3f();

    private static boolean active;

    private DisplayCulling() {
    }

    public static void begin(Matrix4fc modelView) {
        active = LevelProjection.isBound();

        if (active)
            FRUSTUM.set(VIEW_PROJECTION.set(LevelProjection.matrix()).mul(modelView), false);
    }

    public static void end() {
        active = false;
    }

    public static boolean isHidden(ItemStackRenderState item, Matrix4fc pose) {
        if (!active)
            return false;

        AABB box = ((ItemCullingBounds) item).txoptimizations$cullingBounds();

        if (box == null)
            return false;

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (int corner = 0; corner < 8; corner ++) {
            pose.transformPosition(CORNER.set((corner & 1) == 0 ? box.minX : box.maxX, (corner & 2) == 0 ? box.minY : box.maxY, (corner & 4) == 0 ? box.minZ : box.maxZ));

            minX = Math.min(minX, CORNER.x);
            minY = Math.min(minY, CORNER.y);
            minZ = Math.min(minZ, CORNER.z);
            maxX = Math.max(maxX, CORNER.x);
            maxY = Math.max(maxY, CORNER.y);
            maxZ = Math.max(maxZ, CORNER.z);
        }

        return !FRUSTUM.testAab(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
