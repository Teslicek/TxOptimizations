package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.TreeArrayPool;
import net.caffeinemc.mods.sodium.client.render.chunk.tree.Tree;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Tree.class, remap = false)
public abstract class TreeArrayPoolMixin {

    @Unique
    private static final int TREE_LONGS = 4096;

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = TREE_LONGS))
    private int txoptimizations$skipTreeAllocation(int length) {
        return 0;
    }

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/tree/Tree;tree:[J", opcode = Opcodes.PUTFIELD))
    private void txoptimizations$usePooledTree(Tree tree, long[] empty) {
        if (empty.length != 0)
            throw new IllegalStateException("Sodium tree array was allocated with " + empty.length + " entries");

        ((TreeAccessor) tree).txoptimizations$setTree(TreeArrayPool.acquire(TREE_LONGS));
    }
}
