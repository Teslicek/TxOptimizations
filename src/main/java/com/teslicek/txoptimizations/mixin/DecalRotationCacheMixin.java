package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SheetedDecalTextureGenerator.class)
public abstract class DecalRotationCacheMixin {

    @Unique
    private static final Quaternionf[] ROTATIONS = new Quaternionf[Direction.values().length];

    static {
        for (Direction direction : Direction.values())
            ROTATIONS[direction.ordinal()] = direction.getRotation();
    }

    @WrapOperation(method = "setNormal", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Direction;getRotation()Lorg/joml/Quaternionf;"))
    private Quaternionf txoptimizations$cachedRotation(Direction direction, Operation<Quaternionf> original) {
        return ROTATIONS[direction.ordinal()];
    }

    @WrapOperation(method = "setSheetedDecalUv", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Direction;getRotation()Lorg/joml/Quaternionf;"))
    private static Quaternionf txoptimizations$cachedStaticRotation(Direction direction, Operation<Quaternionf> original) {
        return ROTATIONS[direction.ordinal()];
    }
}
