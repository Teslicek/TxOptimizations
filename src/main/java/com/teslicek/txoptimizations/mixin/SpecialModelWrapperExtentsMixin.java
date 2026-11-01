package com.teslicek.txoptimizations.mixin;

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
public abstract class SpecialModelWrapperExtentsMixin {

    @Shadow
    @Final
    private Supplier<Vector3fc[]> extents;

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void txoptimizations$measureWhileBaking(CallbackInfo ci) {
        this.extents.get();
    }
}
