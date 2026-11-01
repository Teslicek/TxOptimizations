package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.teslicek.txoptimizations.ModelCopies;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderLayer.class)
public abstract class RenderLayerTwinMixin {

    @ModifyReturnValue(method = "getParentModel", at = @At("RETURN"), require = 1)
    private EntityModel<?> txoptimizations$parentTwin(EntityModel<?> model) {
        return ModelCopies.mainModel(model);
    }
}
