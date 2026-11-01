package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.teslicek.txoptimizations.mixin.GameRendererBobAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.state.ItemDisplayEntityRenderState;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

public final class DisplayPreCull {

    private static final float HITBOX_REACH = 3.5F;
    private static final float RADIUS_SLACK = 1.0E-3F;

    private static final Matrix4f[]          PROJECTIONS     = {new Matrix4f(), new Matrix4f()};
    private static final Matrix4f[]          VIEWS           = {new Matrix4f(), new Matrix4f()};
    private static final long[]              FRAMES          = {-1L, -1L};
    private static final Matrix4f            VIEW_PROJECTION = new Matrix4f();
    private static final Matrix4f            WIDENED         = new Matrix4f();
    private static final Matrix4f            TRANSFORM       = new Matrix4f();
    private static final FrustumIntersection FRUSTUM         = new FrustumIntersection();
    private static final Vector3f            CORNER          = new Vector3f();
    private static final Vector3f            AXIS            = new Vector3f(0.0F, Mth.SQRT_OF_TWO / 2.0F, Mth.SQRT_OF_TWO / 2.0F);

    private static double cameraX;
    private static double cameraY;
    private static double cameraZ;

    private DisplayPreCull() {
    }

    public static void prepare(GameRenderer renderer) {
        GameRenderState   state   = renderer.gameRenderState();
        CameraRenderState camera  = state.levelRenderState.cameraRenderState;
        PlayerRenderState player  = state.levelRenderState.playerRenderState;
        PoseStack         bob     = new PoseStack();
        float             scale   = state.optionsRenderState.screenEffectScale;
        float             spin    = Math.max(player.portalEffectIntensity, player.nauseaEffectIntensity) * (scale * scale);
        int               parity  = RenderStateBuffers.parity();
        Matrix4f          project = PROJECTIONS[parity];
        Matrix4f          view    = VIEWS[parity];

        ((GameRendererBobAccessor) renderer).txoptimizations$bobHurt(camera, bob);

        if (state.optionsRenderState.bobView)
            ((GameRendererBobAccessor) renderer).txoptimizations$bobView(camera, bob);

        project.set(camera.projectionMatrix).mul(bob.last().pose());

        if (spin > 0.0F) {
            float skew  = 5.0F / (spin * spin + 5.0F) - spin * 0.04F;
            float angle = player.spinningEffectAngle * (float) (Math.PI / 180.0);

            skew *= skew;
            project.rotate(angle, AXIS);
            project.scale(1.0F / skew, 1.0F, 1.0F);
            project.rotate(-angle, AXIS);
        }

        view.set(camera.viewRotationMatrix);
        VIEW_PROJECTION.set(project).mul(view);
        FRUSTUM.set(DisplayCulling.widen(VIEW_PROJECTION, WIDENED), true);

        cameraX        = camera.pos.x;
        cameraY        = camera.pos.y;
        cameraZ        = camera.pos.z;
        FRAMES[parity] = ClientClock.frame();
    }

    public static Matrix4fc projection() {
        return PROJECTIONS[preparedParity()];
    }

    public static Matrix4fc view() {
        return VIEWS[preparedParity()];
    }

    private static int preparedParity() {
        int parity = RenderStateBuffers.parity();

        if (FRAMES[parity] != ClientClock.frame())
            throw new IllegalStateException("Display culling read frame " + ClientClock.frame() + " but pre-culling last prepared frame " + FRAMES[parity]);

        return parity;
    }

    public static void remember(Display.ItemDisplay display, ItemDisplayEntityRenderState state) {
        ItemDisplayCullCache cache       = (ItemDisplayCullCache) display;
        Display.RenderState  renderState = display.renderState();
        Object               models      = Minecraft.getInstance().getModelManager().bakedItemStackModels;

        if (state.interpolationProgress != 1.0F || renderState == null || display.itemRenderState() == null) {
            cache.txoptimizations$forget();
            return;
        }

        if (cache.txoptimizations$matches(renderState, display.itemRenderState(), models))
            return;

        if (plainModel(display) == null) {
            cache.txoptimizations$forget();
            return;
        }

        AABB box = ((ItemCullingBounds) state.item).txoptimizations$cullingBounds();

        if (box == null) {
            cache.txoptimizations$forget();
            return;
        }

        Matrix4fc transform = TRANSFORM.set(renderState.transformation().get(1.0F).getMatrix()).rotate(Axis.YP.rotation((float) Math.PI));
        float     radius    = 0.0F;

        for (int corner = 0; corner < 8; corner ++) {
            transform.transformPosition(CORNER.set((float) ((corner & 1) == 0 ? box.minX : box.maxX), (float) ((corner & 2) == 0 ? box.minY : box.maxY), (float) ((corner & 4) == 0 ? box.minZ : box.maxZ)));
            radius = Math.max(radius, CORNER.length());
        }

        cache.txoptimizations$remember(renderState, display.itemRenderState(), models, radius);
    }

    public static boolean isHidden(Display.ItemDisplay display, float partialTicks) {
        if (FRAMES[RenderStateBuffers.parity()] != ClientClock.frame())
            return false;

        ItemDisplayCullCache cache       = (ItemDisplayCullCache) display;
        Display.RenderState  renderState = display.renderState();

        if (renderState == null || display.hasCustomName() || display.getVehicle() != null || renderState.shadowRadius().get(1.0F) != 0.0F)
            return false;

        if (!cache.txoptimizations$matches(renderState, display.itemRenderState(), Minecraft.getInstance().getModelManager().bakedItemStackModels) || display.calculateInterpolationProgress(partialTicks) != 1.0F)
            return false;

        float  radius = Math.max(cache.txoptimizations$radius(), Math.abs(display.getEyeHeight()) + HITBOX_REACH) + RADIUS_SLACK;
        double x      = Mth.lerp(partialTicks, display.xOld, display.getX()) - cameraX;
        double y      = Mth.lerp(partialTicks, display.yOld, display.getY()) - cameraY;
        double z      = Mth.lerp(partialTicks, display.zOld, display.getZ()) - cameraZ;

        return !FRUSTUM.testSphere((float) x, (float) y, (float) z, radius);
    }

    private static ItemModel plainModel(Display.ItemDisplay display) {
        Display.ItemDisplay.ItemRenderState itemState = display.itemRenderState();

        if (itemState == null)
            return null;

        ItemStack  stack   = itemState.itemStack();
        Identifier modelId = stack.isEmpty() ? null : stack.get(DataComponents.ITEM_MODEL);

        if (modelId == null)
            return null;

        ItemModel model = ItemModelLookup.model(Minecraft.getInstance().getModelManager(), stack.getItem(), modelId);

        return model.getClass() == CuboidItemModelWrapper.class ? model : null;
    }
}
