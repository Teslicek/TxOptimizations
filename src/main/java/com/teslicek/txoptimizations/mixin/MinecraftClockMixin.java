package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ClientClock;
import com.teslicek.txoptimizations.FpsCounter;
import com.teslicek.txoptimizations.FramePacer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftClockMixin {

    @Inject(method = "runTick", at = @At("HEAD"))
    private void txoptimizations$paceFrame(boolean advanceGameTime, CallbackInfo ci) {
        FramePacer.awaitStart();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void txoptimizations$advanceTick(CallbackInfo ci) {
        ClientClock.advanceTick();
    }

    @Inject(method = "renderFrame", at = @At("HEAD"))
    private void txoptimizations$advanceFrame(boolean advanceGameTime, CallbackInfo ci) {
        ClientClock.advanceFrame(Util.getMillis());
        FpsCounter.countFrame();
    }
}
