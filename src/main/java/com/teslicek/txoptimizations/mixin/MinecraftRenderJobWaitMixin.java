package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.RenderWorker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftRenderJobWaitMixin {

    @Shadow
    @Final
    public Gui gui;

    @Inject(method = {"close", "setLevel", "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", "clearClientLevel", "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;)V", "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V", "invalidateSurfaceConfiguration"}, at = @At("HEAD"), require = 8)
    private void txoptimizations$waitBeforeLevelChange(CallbackInfo ci) {
        RenderWorker.awaitFromMain();
    }

    @Inject(method = "runTick", at = @At("HEAD"), require = 1)
    private void txoptimizations$waitWhileReloading(boolean advanceGameTime, CallbackInfo ci) {
        if (this.gui.overlay() != null)
            RenderWorker.awaitFromMain();
    }
}
