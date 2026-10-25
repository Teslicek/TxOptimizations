package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.teslicek.txoptimizations.InFlightVertexCopy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ByteBufferBuilder.class)
public abstract class ByteBufferBuilderResizeMixin {

    @Inject(method = "resize", at = @At("HEAD"))
    private void txoptimizations$awaitCopyBeforeMoving(long newCapacity, CallbackInfo ci) {
        InFlightVertexCopy.awaitBeforeMoving((ByteBufferBuilder) (Object) this);
    }
}
