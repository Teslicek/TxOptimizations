package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ItemModelPurity;
import net.minecraft.client.renderer.item.ConditionalItemModel;
import net.minecraft.client.renderer.item.properties.conditional.ItemModelPropertyTest;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConditionalItemModel.class)
public abstract class ConditionalItemModelPurityMixin {

    @Shadow
    @Final
    private ItemModelPropertyTest property;

    @Inject(method = "update", at = @At("HEAD"))
    private void txoptimizations$checkProperty(CallbackInfo ci) {
        ItemModelPurity.checkProperty(this.property);
    }
}
