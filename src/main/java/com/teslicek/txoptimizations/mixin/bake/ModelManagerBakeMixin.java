package com.teslicek.txoptimizations.mixin.bake;

import com.teslicek.txoptimizations.bake.BakedModels;
import net.minecraft.client.resources.model.ModelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelManager.class)
public abstract class ModelManagerBakeMixin {

    @Inject(method = "apply", at = @At("TAIL"))
    private void txoptimizations$resetBakedModels(CallbackInfo ci) {
        BakedModels.reset();
    }
}
