package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.audio.ListenerTransform;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEngineExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class SoundEngineListenerMixin {

    @Unique
    private static final long MIN_UPDATE_INTERVAL_NANOS = 5_000_000L;

    @Unique
    private ListenerTransform txoptimizations$sentTransform;

    @Unique
    private long txoptimizations$sentNanos;

    @WrapOperation(method = "updateSource", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundEngineExecutor;execute(Ljava/lang/Runnable;)V"))
    private void txoptimizations$sendChangedListener(SoundEngineExecutor executor, Runnable setListener, Operation<Void> original, @Local ListenerTransform transform) {
        if (transform.equals(this.txoptimizations$sentTransform))
            return;

        long now = System.nanoTime();

        if (this.txoptimizations$sentTransform != null && now - this.txoptimizations$sentNanos < MIN_UPDATE_INTERVAL_NANOS)
            return;

        original.call(executor, setListener);
        this.txoptimizations$sentTransform = transform;
        this.txoptimizations$sentNanos     = now;
    }

    @Inject(method = "loadLibrary", at = @At("TAIL"))
    private void txoptimizations$forgetSentListener(CallbackInfo ci) {
        this.txoptimizations$sentTransform = null;
    }
}
