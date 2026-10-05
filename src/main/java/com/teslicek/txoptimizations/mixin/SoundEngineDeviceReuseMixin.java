package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.audio.Library;
import com.teslicek.txoptimizations.ReusableAudioDevice;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SoundEngine.class)
public abstract class SoundEngineDeviceReuseMixin {

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
