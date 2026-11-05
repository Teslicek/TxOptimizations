package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.audio.Library;
import com.teslicek.txoptimizations.ReusableAudioDevice;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

    @WrapOperation(method = "play", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;join()Ljava/lang/Object;"))
    private Object txoptimizations$spinForChannel(CompletableFuture<?> channel, Operation<Object> original) {
        while (!channel.isDone())
            Thread.onSpinWait();

        return original.call(channel);
    }

    @Shadow
    @Final
    private Library library;

    @WrapMethod(method = "reload")
    private void txoptimizations$reuseDeviceDuringReload(Operation<Void> original) {
        ReusableAudioDevice device = (ReusableAudioDevice) this.library;

        device.txoptimizations$setReuseRequested(true);

        try {
            original.call();
        } finally {
            device.txoptimizations$setReuseRequested(false);
        }
    }
}
