package com.teslicek.txoptimizations.mixin.cull;

import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.DirectionalVisGraph;
import net.caffeinemc.mods.sodium.client.util.collections.BitArray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DirectionalVisGraph.class)
public interface DirectionalVisGraphAccessor {

    @Accessor("blocks")
    BitArray txoptimizations$blocks();
}
