package com.teslicek.txoptimizations.mixin;

import java.util.List;
import java.util.function.Function;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Model.class)
public abstract class ModelResetPoseMixin {

    @Shadow
    @Final
    private List<ModelPart> allParts;

    @Unique
    private ModelPart[] txoptimizations$parts;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$snapshotParts(ModelPart root, Function<Identifier, RenderType> renderType, CallbackInfo ci) {
        this.txoptimizations$parts = this.allParts.toArray(ModelPart[]::new);
    }

    @Overwrite
    public final void resetPose() {
        for (ModelPart part : this.txoptimizations$parts)
            part.resetPose();
    }
}
