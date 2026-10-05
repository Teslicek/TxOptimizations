package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teslicek.txoptimizations.AirCollision;
import java.util.List;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Particle.class)
public abstract class ParticleAirCollisionMixin {

    @WrapOperation(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collideBoundingBox(Lnet/minecraft/world/phys/shapes/CollisionContext;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 txoptimizations$skipAirCollision(CollisionContext context, Vec3 movement, AABB box, Level level, List<VoxelShape> colliders, Operation<Vec3> original) {
        if (colliders.isEmpty() && AirCollision.hasNoBlockColliders(level, box.expandTowards(movement)))
            return movement;

        return original.call(context, movement, box, level, colliders);
    }
}
