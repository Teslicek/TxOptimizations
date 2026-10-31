package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ItemModelPurity;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpecialModelWrapper.class)
public abstract class SpecialModelWrapperPurityMixin {

    @Inject(method = "update", at = @At("HEAD"))
    private void txoptimizations$markImpure(CallbackInfo ci) {
        ItemModelPurity.markImpure();
    }
}
