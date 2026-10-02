package com.teslicek.txoptimizations.cull;

public interface RegionOcclusion {

    int[] txoptimizations$getOccluders(int sectionIndex);

    long[] txoptimizations$getHiddenSections();
}
