package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.ReloadTimeline;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ProfiledReloadInstance;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SimpleReloadInstance.class)
public abstract class SimpleReloadInstanceTimelineMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$startTimeline(CallbackInfo ci) {
        if ((Object) this instanceof ProfiledReloadInstance)
            ReloadTimeline.begin();
    }

    @ModifyReturnValue(method = "createBarrierForListener", at = @At("RETURN"))
    private PreparableReloadListener.PreparationBarrier txoptimizations$timePreparation(PreparableReloadListener.PreparationBarrier barrier, @Local(argsOnly = true) PreparableReloadListener listener) {
        if (!((Object) this instanceof ProfiledReloadInstance))
            return barrier;

        return new ReloadTimeline(barrier, listener.getName());
    }
}
