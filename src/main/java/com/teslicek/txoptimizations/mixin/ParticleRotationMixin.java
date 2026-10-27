package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teslicek.txoptimizations.ParticleSnapshot;
import net.minecraft.client.particle.SingleQuadParticle;
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
        return ParticleSnapshot.rotateRoll(rotation, angle);
    }
}
