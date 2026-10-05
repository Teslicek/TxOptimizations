package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
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

@Mixin(EntityHitboxDebugRenderer.class)
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
        AABB   box             = entity.getBoundingBox().move(offsetX, offsetY, offsetZ);

        Gizmos.cuboid(box, isServerEntity ? SERVER_STROKE : CLIENT_STROKE);
        Gizmos.point(currentPosition, mainColor, 2.0F);

        Entity vehicle = entity.getVehicle();

        if (vehicle != null) {
            float width    = Math.min(vehicle.getBbWidth(), entity.getBbWidth()) / 2.0F;
            Vec3  riding   = vehicle.getPassengerRidingPosition(entity);
            double ridingX = riding.x + offsetX;
            double ridingY = riding.y + offsetY;
            double ridingZ = riding.z + offsetZ;

            Gizmos.cuboid(new AABB(ridingX - width, ridingY, ridingZ - width, ridingX + width, ridingY + 0.0625, ridingZ + width), VEHICLE_STROKE);
        }

        if (entity instanceof LivingEntity)
            Gizmos.cuboid(new AABB(box.minX, box.minY + entity.getEyeHeight() - 0.01F, box.minZ, box.maxX, box.minY + entity.getEyeHeight() + 0.01F, box.maxZ), EYE_STROKE);

        if (entity instanceof EnderDragon dragon) {
            for (EnderDragonPart subEntity : dragon.getSubEntities()) {
                Vec3 latestSubPosition  = subEntity.position();
                Vec3 currentSubPosition = subEntity.getPosition(partialTicks);

                Gizmos.cuboid(subEntity.getBoundingBox().move(currentSubPosition.x + -latestSubPosition.x, currentSubPosition.y + -latestSubPosition.y, currentSubPosition.z + -latestSubPosition.z), DRAGON_PART_STROKE);
            }
        }

        Vec3 eyePosition = currentPosition.add(0.0, entity.getEyeHeight(), 0.0);
        Vec3 viewVector  = entity.getViewVector(partialTicks);

        Gizmos.arrow(eyePosition, eyePosition.add(viewVector.x * 2.0, viewVector.y * 2.0, viewVector.z * 2.0), -16776961);

        if (isServerEntity) {
            Vec3 deltaMovement = entity.getDeltaMovement();

            Gizmos.arrow(currentPosition, currentPosition.add(deltaMovement), -256);
        }
    }
}
