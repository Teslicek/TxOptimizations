package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.FrustumBoxTest;
import com.teslicek.txoptimizations.mixin.cull.EntityRendererCullAccessor;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererCullBoxMixin<T extends Entity> {

    @Unique
    private static final double CULLING_MARGIN = 0.5;

    @Unique
    private static final double FALLBACK_RADIUS = 2.0;

    @Shadow
    @Final
    protected EntityRenderDispatcher entityRenderDispatcher;

    @Shadow
    protected abstract boolean affectedByCulling(T entity);

    @Shadow
    protected abstract AABB getBoundingBoxForCulling(T entity, float partialTicks);

    @Overwrite
    public boolean shouldRender(T entity, Frustum culler, double camX, double camY, double camZ, float partialTicks) {
        if (!entity.shouldRender(camX, camY, camZ))
            return false;

        if (!this.affectedByCulling(entity))
            return true;

        AABB   box  = this.getBoundingBoxForCulling(entity, partialTicks);
        double minX = Math.min(box.minX - CULLING_MARGIN, box.maxX + CULLING_MARGIN);
        double minY = Math.min(box.minY - CULLING_MARGIN, box.maxY + CULLING_MARGIN);
        double minZ = Math.min(box.minZ - CULLING_MARGIN, box.maxZ + CULLING_MARGIN);
        double maxX = Math.max(box.minX - CULLING_MARGIN, box.maxX + CULLING_MARGIN);
        double maxY = Math.max(box.minY - CULLING_MARGIN, box.maxY + CULLING_MARGIN);
        double maxZ = Math.max(box.minZ - CULLING_MARGIN, box.maxZ + CULLING_MARGIN);

        if (Double.isNaN(minX) || Double.isNaN(minY) || Double.isNaN(minZ) || Double.isNaN(maxX) || Double.isNaN(maxY) || Double.isNaN(maxZ) || (maxX - minX + (maxY - minY) + (maxZ - minZ)) / 3.0 == 0.0) {
            AABB fallback = new AABB(entity.getX() - FALLBACK_RADIUS, entity.getY() - FALLBACK_RADIUS, entity.getZ() - FALLBACK_RADIUS, entity.getX() + FALLBACK_RADIUS, entity.getY() + FALLBACK_RADIUS, entity.getZ() + FALLBACK_RADIUS);

            minX = fallback.minX;
            minY = fallback.minY;
            minZ = fallback.minZ;
            maxX = fallback.maxX;
            maxY = fallback.maxY;
            maxZ = fallback.maxZ;
        }

        if (((FrustumBoxTest) culler).txoptimizations$isBoxVisible(minX, minY, minZ, maxX, maxY, maxZ))
            return true;

        if (!(entity instanceof Leashable leashable))
            return false;

        Entity leashHolder = leashable.getLeashHolder();

        if (leashHolder == null)
            return false;

        AABB leasherBox = ((EntityRendererCullAccessor) this.entityRenderDispatcher.getRenderer(leashHolder)).txoptimizations$getBoundingBoxForCulling(leashHolder, partialTicks);

        return culler.isVisible(leasherBox) || culler.isVisible(new AABB(minX, minY, minZ, maxX, maxY, maxZ).minmax(leasherBox));
    }
}
