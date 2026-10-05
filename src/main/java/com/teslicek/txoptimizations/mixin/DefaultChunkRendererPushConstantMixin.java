package com.teslicek.txoptimizations.mixin;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DefaultChunkRenderer.class, remap = false)
public abstract class DefaultChunkRendererPushConstantMixin {

    @Unique
    private static ByteBuffer txoptimizations$constants;

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lorg/lwjgl/system/MemoryStack;malloc(I)Ljava/nio/ByteBuffer;"))
    private ByteBuffer txoptimizations$reuseConstants(MemoryStack stack, int size) {
        if (txoptimizations$constants == null || txoptimizations$constants.capacity() < size)
            txoptimizations$constants = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());

        return txoptimizations$constants.clear().limit(size);
    }
}
