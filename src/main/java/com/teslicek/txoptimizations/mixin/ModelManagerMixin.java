package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teslicek.txoptimizations.BackgroundCleanup;
import com.teslicek.txoptimizations.ItemBoundsCache;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelManager.class)
public abstract class ModelManagerMixin {

    @WrapOperation(method = "lambda$loadModels$1", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/sprite/MaterialBaker;logMissingTextures()V"))
    private static void txoptimizations$logMissingTexturesInBackground(MaterialBaker materialBaker, Operation<Void> original) {
        BackgroundCleanup.run(() -> original.call(materialBaker));
    }

    @Inject(method = "apply", at = @At("TAIL"))
    private void txoptimizations$dropItemBounds(CallbackInfo ci) {
        ItemBoundsCache.clear();
    }
}
