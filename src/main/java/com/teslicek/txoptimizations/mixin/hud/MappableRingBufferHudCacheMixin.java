package com.teslicek.txoptimizations.mixin.hud;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.MappableRingBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = MappableRingBuffer.class, priority = 2000)
public abstract class MappableRingBufferHudCacheMixin {

    @Unique
    private static final int HUD_FLUSH_EXTRA = 3;

    @ModifyExpressionValue(method = {"<init>", "close", "rotate"}, at = @At(value = "CONSTANT", args = "intValue=3"))
    private int txoptimizations$extendForHudFlush(int buffers) {
        return buffers + HUD_FLUSH_EXTRA;
    }
}
