package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.particle.SingleQuadParticle;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SingleQuadParticle.class)
public abstract class ParticleRotationMixin {

    @Unique
    private static final Quaternionf ROTATION = new Quaternionf();

    @WrapOperation(method = "extract", at = @At(value = "NEW", target = "()Lorg/joml/Quaternionf;"))
    private Quaternionf txoptimizations$reuseRotation(Operation<Quaternionf> original) {
        return ROTATION.identity();
    }

    @WrapOperation(method = "extract", at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotateZ(F)Lorg/joml/Quaternionf;"))
    private Quaternionf txoptimizations$skipZeroRoll(Quaternionf rotation, float angle, Operation<Quaternionf> original) {
        if (angle == 0.0F)
            return rotation;

        return original.call(rotation, angle);
    }
}
