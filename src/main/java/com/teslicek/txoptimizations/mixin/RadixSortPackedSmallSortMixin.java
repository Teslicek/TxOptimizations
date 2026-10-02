package com.teslicek.txoptimizations.mixin;

import it.unimi.dsi.fastutil.ints.IntArrays;
import java.util.Arrays;
import net.caffeinemc.mods.sodium.client.util.sorting.RadixSort;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = RadixSort.class, remap = false)
public abstract class RadixSortPackedSmallSortMixin {

    @Overwrite
    private static void smallSort(int[] perm, int[] keys, boolean stable) {
        if (perm.length <= 1)
            return;

        if (!stable) {
            IntArrays.quickSortIndirect(perm, keys);
            return;
        }

        long[] packed = new long[perm.length];

        for (int index = 0; index < perm.length; index ++)
            packed[index] = (long) keys[perm[index]] << Integer.SIZE | perm[index] & 0xFFFFFFFFL;

        Arrays.sort(packed);

        for (int index = 0; index < perm.length; index ++)
            perm[index] = (int) packed[index];
    }
}
