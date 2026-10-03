package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.teslicek.txoptimizations.EntityCullOrder;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SodiumWorldRenderer.class, remap = false)
public abstract class SodiumEntityCullOrderMixin {

    @ModifyExpressionValue(method = "isEntityVisible", at = @At(value = "FIELD", target = "Lnet/caffeinemc/mods/sodium/client/render/SodiumWorldRenderer;useEntityCulling:Z"))
    private boolean txoptimizations$deferToVanillaFirst(boolean useEntityCulling) {
        return useEntityCulling && !EntityCullOrder.isDeferring();
    }
}
