package com.teslicek.txoptimizations.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "it.unimi.dsi.fastutil.objects.Object2IntMaps$UnmodifiableMap", remap = false)
public interface UnmodifiableObject2IntMapAccessor {

    @Accessor("map")
    Object2IntMap<String> txoptimizations$map();
}
