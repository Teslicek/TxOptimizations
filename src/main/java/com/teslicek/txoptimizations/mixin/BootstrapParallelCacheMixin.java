package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.BlockStateCacheBatch;
import net.minecraft.server.Bootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Bootstrap.class)
public abstract class BootstrapParallelCacheMixin {

    @Inject(method = "bootStrap", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FireBlock;bootStrap()V", shift = At.Shift.AFTER))
    private static void txoptimizations$buildCachesInParallel(CallbackInfo ci) {
        BlockStateCacheBatch.buildAll();
    }
}
