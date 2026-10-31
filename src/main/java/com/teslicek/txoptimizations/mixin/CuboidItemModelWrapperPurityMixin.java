package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ItemModelPurity;
import java.util.List;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CuboidItemModelWrapper.class)
public abstract class CuboidItemModelWrapperPurityMixin {

    @Shadow
    @Final
    private List<ItemTintSource> tints;

    @Inject(method = "update", at = @At("HEAD"))
    private void txoptimizations$checkTints(CallbackInfo ci) {
        ItemModelPurity.checkTints(this.tints);
    }
}
