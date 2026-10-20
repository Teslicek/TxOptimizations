package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SimpleFeatureRenderPhase.class, priority = 999)
public abstract class SimpleFeatureRenderPhaseEmptyMixin {

    @Unique
    private boolean txoptimizations$hasSubmits;

    @ModifyVariable(method = "submit", at = @At("HEAD"), argsOnly = true)
    private SubmitNode txoptimizations$markSubmitted(SubmitNode submit) {
        this.txoptimizations$hasSubmits = true;

        return submit;
    }

    @Inject(method = "clear", at = @At("TAIL"))
    private void txoptimizations$markCleared(CallbackInfo ci) {
        this.txoptimizations$hasSubmits = false;
    }

    @Overwrite
    public boolean isEmpty() {
        return !this.txoptimizations$hasSubmits;
    }
}
