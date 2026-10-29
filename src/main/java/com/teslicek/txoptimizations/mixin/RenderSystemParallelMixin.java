package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teslicek.txoptimizations.ParallelFeatures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderSystem.class)
public abstract class RenderSystemParallelMixin {

    @ModifyReturnValue(method = "isOnRenderThread", at = @At("RETURN"))
    private static boolean txoptimizations$allowLockedWorker(boolean onRenderThread) {
        return onRenderThread || ParallelFeatures.isPreparingOnWorker();
    }
}
