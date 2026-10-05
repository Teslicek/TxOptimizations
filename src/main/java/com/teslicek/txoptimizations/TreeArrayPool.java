package com.teslicek.txoptimizations;

import com.teslicek.txoptimizations.mixin.BaseBiForestAccessor;
import com.teslicek.txoptimizations.mixin.BaseMultiForestAccessor;
import com.teslicek.txoptimizations.mixin.RayOcclusionSectionTreeAccessor;
import com.teslicek.txoptimizations.mixin.TreeAccessor;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.RayOcclusionSectionTree;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.SectionTree;
import net.caffeinemc.mods.sodium.client.render.chunk.tree.BaseBiForest;
import net.caffeinemc.mods.sodium.client.render.chunk.tree.BaseMultiForest;
import net.caffeinemc.mods.sodium.client.render.chunk.tree.Forest;
import net.caffeinemc.mods.sodium.client.render.chunk.tree.Tree;

public final class TreeArrayPool {

    private static final int                           TREE_LONGS = 4096;
    private static final int                           MAX_POOLED = 64;
    private static final ConcurrentLinkedQueue<long[]> POOL       = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger                 POOLED     = new AtomicInteger();

    private TreeArrayPool() {
    }

    public static long[] acquire(int length) {
        if (length != TREE_LONGS)
            throw new IllegalStateException("Unexpected Sodium tree size " + length);

        long[] pooled = POOL.poll();

        if (pooled == null)
            return new long[TREE_LONGS];

        POOLED.decrementAndGet();
        Arrays.fill(pooled, 0L);

        return pooled;
    }

    public static void release(SectionTree sectionTree) {
        releaseForest(sectionTree.tree);

        if (sectionTree instanceof RayOcclusionSectionTree rayTree)
            releaseForest(((RayOcclusionSectionTreeAccessor) rayTree).txoptimizations$portalTree());
    }

    private static void releaseForest(Forest<?> forest) {
        if (forest instanceof BaseBiForest<?> biForest) {
            BaseBiForestAccessor accessor = (BaseBiForestAccessor) biForest;

            releaseTree(accessor.txoptimizations$mainTree());
            releaseTree(accessor.txoptimizations$secondaryTree());

            return;
        }

        if (forest instanceof BaseMultiForest<?> multiForest) {
            for (Tree tree : ((BaseMultiForestAccessor) multiForest).txoptimizations$trees())
                releaseTree(tree);

            return;
        }

        throw new IllegalStateException("Unexpected Sodium forest " + forest.getClass().getName());
    }

    private static void releaseTree(Tree tree) {
        if (tree == null)
            return;

        if (POOLED.incrementAndGet() > MAX_POOLED) {
            POOLED.decrementAndGet();

            return;
        }

        POOL.add(((TreeAccessor) tree).txoptimizations$tree());
    }
}
