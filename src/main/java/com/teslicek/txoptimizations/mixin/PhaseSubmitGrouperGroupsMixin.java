package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.PhaseGroups;
import java.util.List;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.feature.FeatureRenderDispatcher$PhaseSubmitGrouper")
public abstract class PhaseSubmitGrouperGroupsMixin {

    @Shadow
    @Final
    private List<?> phaseGroups;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$rememberPhaseGroups(CallbackInfo ci, @Local(argsOnly = true) FeatureRenderPhase<?> phase) {
        PhaseGroups.require(phase).txoptimizations$setGroups(this.phaseGroups);
    }
}
