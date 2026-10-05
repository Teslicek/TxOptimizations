package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.CompactVectorArray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CompactVectorArray.class)
public interface CompactVectorArrayAccessor {

    @Accessor("contents")
    float[] txoptimizations$contents();
}
