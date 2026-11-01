package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ModelPartOwner;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Model.class)
public abstract class ModelClaimPartsMixin {

    @Shadow
    @Final
    private List<ModelPart> allParts;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$claimParts(ModelPart root, Function<Identifier, RenderType> renderType, CallbackInfo ci) {
        if (((Object) this).getClass() == Model.Simple.class)
            return;

        for (ModelPart part : this.allParts)
            ((ModelPartOwner) (Object) part).txoptimizations$claim(this);
    }
}
