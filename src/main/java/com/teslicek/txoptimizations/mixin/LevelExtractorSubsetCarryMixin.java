package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SectionSubset;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorSubsetCarryMixin {

    @Shadow
    @Final
    private LevelRenderer levelRenderer;

    @Shadow
    private @Nullable SectionUpdateTracker sectionUpdateTracker;

    @Inject(method = "applyFrustum", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;clearVisibleSections()V"))
    private void txoptimizations$beginWalk(Frustum frustum, CallbackInfo ci) {
        SectionSubset.beginWalk();
    }

    @Inject(method = "applyFrustum", at = @At("RETURN"))
    private void txoptimizations$carrySubsets(Frustum frustum, CallbackInfo ci) {
        ViewArea viewArea = this.levelRenderer.viewArea();

        if (viewArea == null)
            throw new IllegalStateException("Visible sections were walked without a view area");

        SectionSubset.endWalk(this.levelRenderer.visibleSections(), viewArea, this.sectionUpdateTracker);
    }
}
