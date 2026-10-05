package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.particle.SingleQuadParticle;
import org.joml.Options;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SingleQuadParticle.class)
public abstract class ParticleRotationMixin {

    @Unique
    private static final Quaternionf ROTATION = new Quaternionf();

    @WrapOperation(method = "extract", at = @At(value = "NEW", target = "()Lorg/joml/Quaternionf;"))
    private Quaternionf txoptimizations$reuseRotation(Operation<Quaternionf> original) {
        return ROTATION.identity();
    }

    @Redirect(method = "extract", at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotateZ(F)Lorg/joml/Quaternionf;"))
    private Quaternionf txoptimizations$rotateRoll(Quaternionf rotation, float angle) {
        if (angle != 0.0F || Options.FASTMATH)
            return rotation.rotateZ(angle);

        float sin = angle * 0.5F;
        float cos = 1.0F;

        return rotation.set(rotation.x * cos + rotation.y * sin, rotation.y * cos - rotation.x * sin, rotation.w * sin + rotation.z * cos, rotation.w * cos - rotation.z * sin);
    }
}
