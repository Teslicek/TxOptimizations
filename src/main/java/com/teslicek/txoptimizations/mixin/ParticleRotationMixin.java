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
}
