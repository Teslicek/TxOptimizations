package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.FramerateLimiter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.locks.LockSupport;

@Mixin(FramerateLimiter.class)
public class FramerateLimiterMixin {

    @Unique
    private static long lastFrameTime = System.nanoTime();

    @Inject(method = "limitDisplayFPS", at = @At("HEAD"), cancellable = true)
    private static void txoptimizations$preciseLimit(int fps, CallbackInfo ci) {
        ci.cancel();

        long targetInterval = 1_000_000_000L / fps;
        long now = System.nanoTime();
        long nextTick = lastFrameTime + targetInterval;

        if (now - nextTick > targetInterval) {
            nextTick = now;
        }

        final long parkThreshold = 500_000L;

        while (true) {
            now = System.nanoTime();
            long remaining = nextTick - now;

            if (remaining <= 0) {
                break;
            }

            if (remaining > parkThreshold) {
                LockSupport.parkNanos(remaining - 100_000L);
            } else {
                Thread.onSpinWait();
            }
        }

        lastFrameTime = nextTick;
    }
}
