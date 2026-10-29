package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ItemBoundsCache;
import net.minecraft.client.resources.model.ModelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelManager.class)
public abstract class ModelManagerItemBoundsMixin {

    @Inject(method = "apply", at = @At("TAIL"))
    private void txoptimizations$dropItemBounds(CallbackInfo ci) {
        ItemBoundsCache.clear();
    }
}
