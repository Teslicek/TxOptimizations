package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.audio.DeviceList;
import com.mojang.blaze3d.audio.Library;
import com.teslicek.txoptimizations.ReusableAudioDevice;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.lwjgl.openal.ALC10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Library.class)
public abstract class LibraryDeviceReuseMixin implements ReusableAudioDevice {

    @Shadow
    private long currentDevice;

    @Shadow
    private long context;

    @Unique
    private boolean txoptimizations$reuseRequested;

    @Unique
    private boolean txoptimizations$deviceKept;

    @Unique
    private boolean txoptimizations$opened;

    @Unique
    private @Nullable String txoptimizations$openedPreferredDevice;

    @Unique
    private DeviceList txoptimizations$openedDevices;

    @Unique
    private boolean txoptimizations$openedHrtf;

    @Shadow
    public abstract boolean isCurrentDeviceDisconnected();

    @Override
    public void txoptimizations$setReuseRequested(boolean requested) {
        this.txoptimizations$reuseRequested = requested;
    }

    @WrapMethod(method = "cleanup")
    private void txoptimizations$keepDevice(Operation<Void> original) {
        this.txoptimizations$deviceKept = this.txoptimizations$reuseRequested && this.txoptimizations$opened && !this.isCurrentDeviceDisconnected();

        original.call();
    }

    @WrapOperation(method = "cleanup", at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/ALC10;alcDestroyContext(J)V"))
    private void txoptimizations$keepContext(long context, Operation<Void> original) {
        if (this.txoptimizations$deviceKept)
            return;

        original.call(context);
    }

    @WrapOperation(method = "cleanup", at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/ALC10;alcCloseDevice(J)Z"))
    private boolean txoptimizations$keepOpenDevice(long device, Operation<Boolean> original) {
        if (this.txoptimizations$deviceKept)
            return true;

        return original.call(device);
    }

    @WrapMethod(method = "init")
    private void txoptimizations$reuseDevice(@Nullable String preferredDevice, DeviceList currentDevices, boolean useHrtf, Operation<Void> original) {
        if (this.txoptimizations$deviceKept) {
            this.txoptimizations$deviceKept = false;

            if (Objects.equals(preferredDevice, this.txoptimizations$openedPreferredDevice) && currentDevices.equals(this.txoptimizations$openedDevices) && useHrtf == this.txoptimizations$openedHrtf)
                return;

            ALC10.alcDestroyContext(this.context);
            ALC10.alcCloseDevice(this.currentDevice);
        }

        this.txoptimizations$opened = false;

        original.call(preferredDevice, currentDevices, useHrtf);

        this.txoptimizations$opened                = true;
        this.txoptimizations$openedPreferredDevice = preferredDevice;
        this.txoptimizations$openedDevices         = currentDevices;
        this.txoptimizations$openedHrtf            = useHrtf;
    }
}
