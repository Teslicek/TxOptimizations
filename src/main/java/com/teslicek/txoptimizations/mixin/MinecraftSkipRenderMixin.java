package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerReconfigScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftSkipRenderMixin {

    @Unique
    private static final long FREEZE_TIMEOUT_MS = 5_000L;

    @Unique
    private static long startMs = 0L;

    @Unique
    private static boolean abandoned = false;

    @Inject(method = "renderFrame(Z)V", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$freezeDuringReconfig(boolean advanceGameTime, CallbackInfo ci) {
        if (txoptimizations$shouldFreeze())
            ci.cancel();
    }

    @Unique
    private static boolean txoptimizations$shouldFreeze() {
        Minecraft mc        = Minecraft.getInstance();
        boolean   inReconfig = mc.gui.screen() instanceof ServerReconfigScreen;

        if ((mc.level != null && !inReconfig) || mc.gui.screen() instanceof TitleScreen) {
            startMs    = 0L;
            abandoned  = false;
            return false;
        }

        if (!inReconfig) {
            if (startMs != 0L)
                abandoned = true;

            return false;
        }

        if (abandoned)
            return false;

        if (mc.gui.overlay() != null) {
            abandoned = true;
            return false;
        }

        long now = System.currentTimeMillis();

        if (startMs == 0L)
            startMs = now;

        return now - startMs < FREEZE_TIMEOUT_MS;
    }
}
