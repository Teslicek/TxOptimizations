package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ClientClock;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftClockMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void txoptimizations$advanceTick(CallbackInfo ci) {
        ClientClock.advanceTick();
    }

    @Inject(method = "renderFrame", at = @At("HEAD"))
    private void txoptimizations$advanceFrame(boolean advanceGameTime, CallbackInfo ci) {
        ClientClock.advanceFrame();
    }
}
