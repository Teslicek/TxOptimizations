package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ItemModelPurity;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RangeSelectItemModel.class)
public abstract class RangeSelectItemModelPurityMixin {

    @Shadow
    @Final
    private RangeSelectItemModelProperty property;

    @Inject(method = "update", at = @At("HEAD"))
    private void txoptimizations$checkProperty(CallbackInfo ci) {
        ItemModelPurity.checkProperty(this.property);
    }
}
