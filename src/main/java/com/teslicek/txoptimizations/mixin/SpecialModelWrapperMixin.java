package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ItemModelPurity;
import java.util.function.Supplier;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpecialModelWrapper.class)
public abstract class SpecialModelWrapperMixin {

    @Inject(method = "update", at = @At("HEAD"))
    private void txoptimizations$markImpure(CallbackInfo ci) {
        ItemModelPurity.markImpure();
    }

    @Shadow
    @Final
    private Supplier<Vector3fc[]> extents;

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void txoptimizations$measureWhileBaking(CallbackInfo ci) {
        this.extents.get();
    }
}
