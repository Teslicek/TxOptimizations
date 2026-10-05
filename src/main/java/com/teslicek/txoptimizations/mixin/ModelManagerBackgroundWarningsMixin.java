package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teslicek.txoptimizations.BackgroundCleanup;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelManager.class)
public abstract class ModelManagerBackgroundWarningsMixin {

    @WrapOperation(method = "lambda$loadModels$1", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/sprite/MaterialBaker;logMissingTextures()V"))
    private static void txoptimizations$logMissingTexturesInBackground(MaterialBaker materialBaker, Operation<Void> original) {
        BackgroundCleanup.run(() -> original.call(materialBaker));
    }
}
