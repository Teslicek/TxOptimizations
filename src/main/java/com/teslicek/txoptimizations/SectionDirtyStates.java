package com.teslicek.txoptimizations;

import net.minecraft.client.RotatingSectionStorage;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.core.SectionPos;
import org.jspecify.annotations.Nullable;

public final class SectionDirtyStates {

    private SectionDirtyStates() {
    }

    public static SectionUpdateTracker.@Nullable SectionDirtyState of(SectionUpdateTracker tracker, int index, long sectionNode) {
        SectionUpdateTracker.SectionDirtyState state = atIndex(tracker.storage, index, sectionNode);

        if (state != null)
            return state;

        return tracker.getDirtyState(sectionNode);
    }

    private static SectionUpdateTracker.@Nullable SectionDirtyState atIndex(RotatingSectionStorage<SectionUpdateTracker.SectionDirtyState> storage, int index, long sectionNode) {
        RotatingSectionStorage.Node<SectionUpdateTracker.SectionDirtyState>[] nodes = storage.nodes;

        if (index < 0 || index >= nodes.length)
            return null;

        SectionUpdateTracker.SectionDirtyState state = nodes[index].value();

        if (state.getSectionNode() != sectionNode || !storage.containsSection(SectionPos.x(sectionNode), SectionPos.y(sectionNode), SectionPos.z(sectionNode)))
            return null;

        return state;
    }
}
