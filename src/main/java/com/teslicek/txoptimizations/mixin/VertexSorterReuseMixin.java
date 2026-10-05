package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.CompactVectorArray;
import com.teslicek.txoptimizations.QuadSort;
import com.teslicek.txoptimizations.SharedQuadSorter;
import java.util.Arrays;
import net.caffeinemc.mods.sodium.client.util.MathUtil;
import net.caffeinemc.mods.sodium.client.util.sorting.VertexSortingExtended;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.util.sorting.VertexSorters$AbstractSorter", remap = false)
public abstract class VertexSorterReuseMixin implements SharedQuadSorter {

    @Overwrite
    public final int[] sort(CompactVectorArray centroids) {
        return Arrays.copyOf(this.txoptimizations$sortShared(centroids), centroids.size());
    }

    @Override
    public int[] txoptimizations$sortShared(CompactVectorArray centroids) {
        VertexSortingExtended sorting = (VertexSortingExtended) (Object) this;
        int                   length  = centroids.size();
        int[]                 keys    = QuadSort.keys(length);

        for (int index = 0; index < length; index ++)
            keys[index] = ~MathUtil.floatToComparableInt(sorting.applyMetric(centroids.getX(index), centroids.getY(index), centroids.getZ(index)));

        return QuadSort.sort(keys, length);
    }
}
