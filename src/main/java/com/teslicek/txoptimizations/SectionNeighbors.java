package com.teslicek.txoptimizations;

import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.multiplayer.ClientLevel;

public final class SectionNeighbors {

    private static volatile int generation = 1;

    private SectionNeighbors() {
    }

    public static void changed() {
        generation ++;
    }

    public static boolean hasAllNeighbors(SectionUpdateTracker tracker, ClientLevel level, long sectionNode, NeighborMark mark) {
        int current = generation;

        if (mark.txoptimizations$getMissingNeighborsGeneration() == current && mark.txoptimizations$getMissingNeighborsNode() == sectionNode)
            return false;

        boolean complete = tracker.hasAllNeighbors(level, sectionNode);

        if (!complete)
            mark.txoptimizations$markMissingNeighbors(sectionNode, current);

        return complete;
    }
}
