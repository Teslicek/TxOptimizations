package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ByteBufferBuilder.class)
public interface ByteBufferBuilderAccessor {

    @Accessor("pointer")
    long txoptimizations$pointer();

    @Invoker("isValid")
    boolean txoptimizations$isValid(int generation);
}
