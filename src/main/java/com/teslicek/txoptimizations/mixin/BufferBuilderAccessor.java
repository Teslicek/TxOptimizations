package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BufferBuilder.class)
public interface BufferBuilderAccessor {

    @Invoker("putVec3f")
    static void txoptimizations$putVec3f(long pointer, float x, float y, float z) {
        throw new AssertionError();
    }

    @Invoker("putRgba")
    static void txoptimizations$putRgba(long pointer, int argb) {
        throw new AssertionError();
    }

    @Invoker("putPackedUv")
    static void txoptimizations$putPackedUv(long pointer, int packedUv) {
        throw new AssertionError();
    }
}
