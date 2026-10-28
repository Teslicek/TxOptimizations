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
import org.jspecify.annotations.Nullable;

public final class DisplayCulling {

    private static final int   TEXT_LINE_HEIGHT = 10;
    private static final float TEXT_MARGIN      = 2.0F;
    private static final float GIZMO_MARGIN     = 16.0F;

    private static final Matrix4f            VIEW_PROJECTION = new Matrix4f();
    private static final Matrix4f            GIZMO_CLIP      = new Matrix4f();
    private static final FrustumIntersection FRUSTUM         = new FrustumIntersection();
    private static final FrustumIntersection GIZMO_FRUSTUM   = new FrustumIntersection();
    private static final float[]             CORNER_X        = new float[CubeVertices.CORNERS];
    private static final float[]             CORNER_Y        = new float[CubeVertices.CORNERS];
    private static final float[]             CORNER_Z        = new float[CubeVertices.CORNERS];

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

    public static boolean isGizmoHidden(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (!active)
            return false;

        return !GIZMO_FRUSTUM.testAab((float) (minX - cameraX), (float) (minY - cameraY), (float) (minZ - cameraZ), (float) (maxX - cameraX), (float) (maxY - cameraY), (float) (maxZ - cameraZ));
    }

    public static boolean isTextHidden(Font font, Matrix4fc pose, Display.TextDisplay.CachedInfo info, int backgroundColor, int width, int height, boolean shadow, Display.TextDisplay.Align alignment, int color) {
        if (!active || info.lines().isEmpty())
            return false;

        TextCullingSlot slot       = (TextCullingSlot) (Object) info;
        TextCullingBox  box        = slot.txoptimizations$cullingBox();
        int             generation = TextLayoutCache.generation();

        if (box == null || !box.isFor(font, generation, backgroundColor, width, height, shadow, alignment, color)) {
            box = textBox(font, info, generation, backgroundColor, width, height, shadow, alignment, color);

            if (box == null)
                return false;

            slot.txoptimizations$setCullingBox(box);
        }

        if (box.minX() > box.maxX())
            return false;

        return isBoxHidden(pose, box.minX(), box.minY(), 0.0F, box.maxX(), box.maxY(), 0.0F);
    }

    public static boolean isVanilla(Object renderer) {
        return renderer.getClass().getName().startsWith("net.minecraft.");
    }

    private static @Nullable TextCullingBox textBox(Font font, Display.TextDisplay.CachedInfo info, int generation, int backgroundColor, int width, int height, boolean shadow, Display.TextDisplay.Align alignment, int color) {
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
                return null;

            ScreenRectangle bounds = text.bounds();

            if (bounds != null) {
                minX = Math.min(minX, bounds.left() - TEXT_MARGIN);
                minY = Math.min(minY, bounds.top() - TEXT_MARGIN);
                maxX = Math.max(maxX, bounds.right() + TEXT_MARGIN);
                maxY = Math.max(maxY, bounds.bottom() + TEXT_MARGIN);
            }

            y += TEXT_LINE_HEIGHT;
        }

        return new TextCullingBox(font, generation, backgroundColor, width, height, shadow, alignment, color, minX, minY, maxX, maxY);
    }

    private static boolean isBoxHidden(Matrix4fc pose, float boxMinX, float boxMinY, float boxMinZ, float boxMaxX, float boxMaxY, float boxMaxZ) {
        CubeVertices.transformCorners(pose, boxMinX, boxMinY, boxMinZ, boxMaxX, boxMaxY, boxMaxZ, CORNER_X, CORNER_Y, CORNER_Z);

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (int corner = 0; corner < CORNER_X.length; corner ++) {
            minX = Math.min(minX, CORNER_X[corner]);
            minY = Math.min(minY, CORNER_Y[corner]);
            minZ = Math.min(minZ, CORNER_Z[corner]);
            maxX = Math.max(maxX, CORNER_X[corner]);
            maxY = Math.max(maxY, CORNER_Y[corner]);
            maxZ = Math.max(maxZ, CORNER_Z[corner]);
        }

        return !FRUSTUM.testAab(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
