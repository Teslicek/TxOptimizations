package com.teslicek.txoptimizations.cull;

public interface RegionOcclusion {

    long[] txoptimizations$getHiddenSections();

    long[] txoptimizations$getTestedSections();

    int txoptimizations$getTestedGeneration();

    void txoptimizations$setTestedGeneration(int generation);
}
