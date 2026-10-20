package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.HitboxGizmos;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = EntityHitboxDebugRenderer.class, priority = 999)
public abstract class EntityHitboxDebugRendererMixin {

    @Unique
    private static final GizmoStyle CLIENT_STROKE = GizmoStyle.stroke(-1);

    @Unique
    private static final GizmoStyle SERVER_STROKE = GizmoStyle.stroke(-16711936);

    @Unique
    private static final GizmoStyle VEHICLE_STROKE = GizmoStyle.stroke(-256);

    @Unique
    private static final GizmoStyle EYE_STROKE = GizmoStyle.stroke(-65536);

    @Unique
    private static final GizmoStyle DRAGON_PART_STROKE = GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 0.25F, 1.0F, 0.0F));

    @Overwrite
    private void showHitboxes(Entity entity, float partialTicks, boolean isServerEntity) {
        Vec3   latestPosition  = entity.position();
        Vec3   currentPosition = entity.getPosition(partialTicks);
        double offsetX         = currentPosition.x + -latestPosition.x;
        double offsetY         = currentPosition.y + -latestPosition.y;
        double offsetZ         = currentPosition.z + -latestPosition.z;
        int    mainColor       = isServerEntity ? -16711936 : -1;
        AABB   bounds          = entity.getBoundingBox();
        double minX            = Math.min(bounds.minX + offsetX, bounds.maxX + offsetX);
        double minY            = Math.min(bounds.minY + offsetY, bounds.maxY + offsetY);
        double minZ            = Math.min(bounds.minZ + offsetZ, bounds.maxZ + offsetZ);
        double maxX            = Math.max(bounds.minX + offsetX, bounds.maxX + offsetX);
        double maxY            = Math.max(bounds.minY + offsetY, bounds.maxY + offsetY);
        double maxZ            = Math.max(bounds.minZ + offsetZ, bounds.maxZ + offsetZ);

        HitboxGizmos.cuboid(minX, minY, minZ, maxX, maxY, maxZ, isServerEntity ? SERVER_STROKE : CLIENT_STROKE);
        HitboxGizmos.point(currentPosition, mainColor, 2.0F);

        Entity vehicle = entity.getVehicle();

        if (vehicle != null) {
            float  width   = Math.min(vehicle.getBbWidth(), entity.getBbWidth()) / 2.0F;
            Vec3   riding  = vehicle.getPassengerRidingPosition(entity);
            double ridingX = riding.x + offsetX;
            double ridingY = riding.y + offsetY;
            double ridingZ = riding.z + offsetZ;

            HitboxGizmos.cuboid(ridingX - width, ridingY, ridingZ - width, ridingX + width, ridingY + 0.0625, ridingZ + width, VEHICLE_STROKE);
        }

        if (entity instanceof LivingEntity)
            HitboxGizmos.cuboid(minX, minY + entity.getEyeHeight() - 0.01F, minZ, maxX, minY + entity.getEyeHeight() + 0.01F, maxZ, EYE_STROKE);

        if (entity instanceof EnderDragon dragon) {
            for (EnderDragonPart subEntity : dragon.getSubEntities()) {
                Vec3   latestSubPosition  = subEntity.position();
                Vec3   currentSubPosition = subEntity.getPosition(partialTicks);
                double subOffsetX         = currentSubPosition.x + -latestSubPosition.x;
                double subOffsetY         = currentSubPosition.y + -latestSubPosition.y;
                double subOffsetZ         = currentSubPosition.z + -latestSubPosition.z;
                AABB   subBounds          = subEntity.getBoundingBox();

                HitboxGizmos.cuboid(subBounds.minX + subOffsetX, subBounds.minY + subOffsetY, subBounds.minZ + subOffsetZ, subBounds.maxX + subOffsetX, subBounds.maxY + subOffsetY, subBounds.maxZ + subOffsetZ, DRAGON_PART_STROKE);
            }
        }

        double eyeX       = currentPosition.x + 0.0;
        double eyeY       = currentPosition.y + entity.getEyeHeight();
        double eyeZ       = currentPosition.z + 0.0;
        Vec3   viewVector = entity.getViewVector(partialTicks);

        HitboxGizmos.arrow(eyeX, eyeY, eyeZ, eyeX + viewVector.x * 2.0, eyeY + viewVector.y * 2.0, eyeZ + viewVector.z * 2.0, -16776961);

        if (isServerEntity) {
            Vec3 deltaMovement = entity.getDeltaMovement();

            HitboxGizmos.arrow(currentPosition.x, currentPosition.y, currentPosition.z, currentPosition.x + deltaMovement.x, currentPosition.y + deltaMovement.y, currentPosition.z + deltaMovement.z, -256);
        }
    }
}
