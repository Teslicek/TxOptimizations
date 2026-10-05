package com.teslicek.txoptimizations.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.tree.BaseBiForest;
import net.caffeinemc.mods.sodium.client.render.chunk.tree.Tree;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = BaseBiForest.class, remap = false)
public interface BaseBiForestAccessor {

    @Accessor("mainTree")
    Tree txoptimizations$mainTree();

    @Accessor("secondaryTree")
    Tree txoptimizations$secondaryTree();
}
