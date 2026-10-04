package com.teslicek.txoptimizations.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.tree.BaseMultiForest;
import net.caffeinemc.mods.sodium.client.render.chunk.tree.Tree;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = BaseMultiForest.class, remap = false)
public interface BaseMultiForestAccessor {

    @Accessor("trees")
    Tree[] txoptimizations$trees();
}
