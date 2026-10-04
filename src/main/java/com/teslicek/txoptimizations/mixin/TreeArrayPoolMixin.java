package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.TreeArrayPool;
import net.caffeinemc.mods.sodium.client.render.chunk.tree.Tree;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Tree.class, remap = false)
public abstract class TreeArrayPoolMixin {

    @Unique
    private static final int TREE_LONGS = 4096;

    @Shadow
    @Final
    @Mutable
    protected long[] tree;

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = TREE_LONGS))
    private int txoptimizations$skipTreeAllocation(int length) {
        return 0;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$usePooledTree(CallbackInfo ci) {
        if (this.tree.length != 0)
            throw new IllegalStateException("Sodium tree array was allocated with " + this.tree.length + " entries");

        this.tree = TreeArrayPool.acquire(TREE_LONGS);
    }
}
