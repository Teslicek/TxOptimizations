package com.teslicek.txoptimizations;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

public final class DisplayCulling {

    private static final int   TEXT_LINE_HEIGHT = 10;
    private static final float TEXT_MARGIN      = 2.0F;
    private static final float GIZMO_MARGIN     = 16.0F;

    private static final Matrix4f            VIEW_PROJECTION = new Matrix4f();
    private static final Matrix4f            GIZMO_CLIP      = new Matrix4f();
    private static final FrustumIntersection FRUSTUM         = new FrustumIntersection();
    private static final FrustumIntersection GIZMO_FRUSTUM   = new FrustumIntersection();
    private static final Vector3f            CORNER          = new Vector3f();

    private static boolean active;
    private static double  cameraX;
    private static double  cameraY;
    private static double  cameraZ;

    private DisplayCulling() {
    }

    public static void begin(Matrix4fc modelView, Vec3 camera) {
        active = LevelProjection.isBound();

        if (!active)
            return;

        DisplayPreCull.verify(LevelProjection.matrix(), modelView);
        FRUSTUM.set(VIEW_PROJECTION.set(LevelProjection.matrix()).mul(modelView), false);
        GIZMO_FRUSTUM.set(widen(VIEW_PROJECTION, GIZMO_CLIP), false);

        cameraX = camera.x;
        cameraY = camera.y;
        cameraZ = camera.z;
    }

    public static Matrix4f widen(Matrix4fc viewProjection, Matrix4f dest) {
        int width  = Minecraft.getInstance().gameRenderer.mainRenderTarget().width;
        int height = Minecraft.getInstance().gameRenderer.mainRenderTarget().height;

        return dest.scaling(width / (width + 2.0F * GIZMO_MARGIN), height / (height + 2.0F * GIZMO_MARGIN), 1.0F).mul(viewProjection);
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

        return isBoxHidden(pose, (float) box.minX, (float) box.minY, (float) box.minZ, (float) box.maxX, (float) box.maxY, (float) box.maxZ);
    }

    public static boolean isGizmoHidden(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double padding) {
        if (!active)
            return false;

        return !GIZMO_FRUSTUM.testAab((float) (minX - padding - cameraX), (float) (minY - padding - cameraY), (float) (minZ - padding - cameraZ), (float) (maxX + padding - cameraX), (float) (maxY + padding - cameraY), (float) (maxZ + padding - cameraZ));
    }

    public static boolean isTextHidden(Font font, Matrix4fc pose, Display.TextDisplay.CachedInfo info, int backgroundColor, int width, int height, boolean shadow, Display.TextDisplay.Align alignment, int color) {
        if (!active || info.lines().isEmpty())
            return false;

        float minX = backgroundColor != 0 ? -1.0F : Float.POSITIVE_INFINITY;
        float minY = backgroundColor != 0 ? -1.0F : Float.POSITIVE_INFINITY;
        float maxX = backgroundColor != 0 ? width : Float.NEGATIVE_INFINITY;
        float maxY = backgroundColor != 0 ? height : Float.NEGATIVE_INFINITY;
        float y    = 0.0F;

        for (Display.TextDisplay.CachedLine line : info.lines()) {
            float offset = switch (alignment) {
                case LEFT -> 0.0F;
                case RIGHT -> width - line.width();
                case CENTER -> width / 2.0F - line.width() / 2.0F;
            };

            Font.PreparedText text = TextLayoutCache.find(font, line.contents(), offset, y, color, shadow, 0, false);

            if (text == null)
                return false;

            ScreenRectangle bounds = text.bounds();

            if (bounds != null) {
                minX = Math.min(minX, bounds.left() - TEXT_MARGIN);
                minY = Math.min(minY, bounds.top() - TEXT_MARGIN);
                maxX = Math.max(maxX, bounds.right() + TEXT_MARGIN);
                maxY = Math.max(maxY, bounds.bottom() + TEXT_MARGIN);
            }

            y += TEXT_LINE_HEIGHT;
        }

        if (minX > maxX)
            return false;

        return isBoxHidden(pose, minX, minY, 0.0F, maxX, maxY, 0.0F);
    }

    public static boolean isVanilla(Object renderer) {
        return renderer.getClass().getName().startsWith("net.minecraft.");
    }

    private static boolean isBoxHidden(Matrix4fc pose, float boxMinX, float boxMinY, float boxMinZ, float boxMaxX, float boxMaxY, float boxMaxZ) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (int corner = 0; corner < 8; corner ++) {
            pose.transformPosition(CORNER.set((corner & 1) == 0 ? boxMinX : boxMaxX, (corner & 2) == 0 ? boxMinY : boxMaxY, (corner & 4) == 0 ? boxMinZ : boxMaxZ));

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
