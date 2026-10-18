package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SectionSubset;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererVisibleSectionsMixin {

    @Inject(method = "clearVisibleSections", at = @At("HEAD"))
    private void txoptimizations$beginClear(CallbackInfo ci) {
        SectionSubset.beginVisibleChange();
    }

    @Inject(method = "clearVisibleSections", at = @At("RETURN"))
    private void txoptimizations$endClear(CallbackInfo ci) {
        SectionSubset.endVisibleChange();
    }
}
