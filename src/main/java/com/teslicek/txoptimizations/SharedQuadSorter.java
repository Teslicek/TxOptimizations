package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.CompactVectorArray;

public interface SharedQuadSorter {

    int[] txoptimizations$sortShared(CompactVectorArray centroids);
}
