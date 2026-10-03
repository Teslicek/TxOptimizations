package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ByteBufferBuilder.Result.class)
public interface ByteBufferResultAccessor {

    @Accessor("offset")
    long txoptimizations$offset();

    @Accessor("generation")
    int txoptimizations$generation();
}
