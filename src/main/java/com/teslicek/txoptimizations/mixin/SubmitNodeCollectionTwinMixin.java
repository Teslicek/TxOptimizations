package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ModelCopies;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SubmitNodeCollection.class)
public abstract class SubmitNodeCollectionTwinMixin {

    @ModifyVariable(method = {"submitModel", "submitCrumblingOverlay"}, at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 2)
    private Model<?> txoptimizations$submitOriginal(Model<?> model) {
        return ModelCopies.submittedModel(model);
    }
}
