package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ModelCopies;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntityRenderer.class)
public abstract class ModelTwinsMixin {

    @Redirect(method = "submit", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;model:Lnet/minecraft/client/model/EntityModel;", opcode = Opcodes.GETFIELD, ordinal = 1), require = 1)
    private EntityModel<?> txoptimizations$poseTwinForLayers(LivingEntityRenderer<?, ?, ?> renderer) {
        return ModelCopies.mainModel(renderer.getModel());
    }
}
