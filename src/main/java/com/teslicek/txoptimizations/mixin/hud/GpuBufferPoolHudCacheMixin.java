package com.teslicek.txoptimizations.mixin.hud;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.teslicek.txoptimizations.hud.HudCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.renderer.StagedVertexBuffer$GpuBufferPool")
public abstract class GpuBufferPoolHudCacheMixin {

    @ModifyExpressionValue(method = "endFrame", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
    private boolean txoptimizations$keepBuffersDuringHudFlush(boolean empty) {
        return empty || HudCache.isFlushing();
    }
}
