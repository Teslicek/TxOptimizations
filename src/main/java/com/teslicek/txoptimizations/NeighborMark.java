package com.teslicek.txoptimizations;

public interface NeighborMark {

    long txoptimizations$getMissingNeighborsNode();

    int txoptimizations$getMissingNeighborsGeneration();

    void txoptimizations$markMissingNeighbors(long sectionNode, int generation);
}
