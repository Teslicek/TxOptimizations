package com.teslicek.txoptimizations.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.SharedQuadIndexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SharedQuadIndexBuffer.class)
public interface SharedQuadIndexBufferAccessor {

    @Accessor("maxPrimitives")
    int txoptimizations$maxPrimitives();
}
