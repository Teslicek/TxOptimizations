package com.teslicek.txoptimizations.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.tree.Tree;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Tree.class, remap = false)
public interface TreeAccessor {

    @Accessor("tree")
    long[] txoptimizations$tree();

    @Mutable
    @Accessor("tree")
    void txoptimizations$setTree(long[] tree);
}
