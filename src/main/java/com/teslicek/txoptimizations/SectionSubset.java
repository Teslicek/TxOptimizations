package com.teslicek.txoptimizations;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.jspecify.annotations.Nullable;

public final class SectionSubset {

    public static final SectionSubset BLOCK_ENTITIES = new SectionSubset(1);
    public static final SectionSubset DIRTY          = new SectionSubset(2);

    private static final long UNSETTLED   = -1L;
    private static final int  FLAG_BITS   = 2;
    private static final int  GENERATIONS = 1 << (Integer.SIZE - FLAG_BITS);

    private static int     generation = 1;
    private static boolean walking;
    private static boolean discarded;

    private final AtomicLong started  = new AtomicLong();
    private final AtomicLong finished = new AtomicLong();
    private final int        flag;

    private ObjectArrayList<SectionRenderDispatcher.RenderSection> subset   = new ObjectArrayList<>();
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> recorded = new ObjectArrayList<>();
    private long                                                   stamp    = UNSETTLED;

    private SectionSubset(int flag) {
        this.flag = flag;
    }

    public void beginChange() {
        this.started.incrementAndGet();
    }

    public void endChange() {
        this.finished.incrementAndGet();
    }

    public void markChange() {
        this.beginChange();
        this.endChange();
    }

    public ObjectListIterator<SectionRenderDispatcher.RenderSection> iterator(ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections) {
        long    stamp = this.settledStamp();
        boolean reuse = stamp != UNSETTLED && stamp == this.stamp;

        ObjectArrayList<SectionRenderDispatcher.RenderSection> previous = this.recorded;

        this.recorded = this.subset;
        this.subset   = previous;
        this.stamp    = stamp;

        this.recorded.clear();

        if (reuse)
            return previous.iterator();

        return visibleSections.iterator();
    }

    public void note(SectionRenderDispatcher.RenderSection section, boolean member) {
        SectionMark mark  = (SectionMark) section;
        int         value = mark.txoptimizations$getSubsetMark();

        mark.txoptimizations$setSubsetMark(member ? value | this.flag : value & ~this.flag);

        if (member)
            this.recorded.add(section);
    }

    private long settledStamp() {
        long finished = this.finished.get();
        long started  = this.started.get();

        return finished == started ? started : UNSETTLED;
    }

    private boolean carriesFlags(long stamp) {
        return stamp != UNSETTLED && stamp == this.stamp;
    }

    private ObjectArrayList<SectionRenderDispatcher.RenderSection> beginCarry() {
        this.recorded.clear();

        return this.recorded;
    }

    private void endCarry(long stamp) {
        this.stamp = stamp != UNSETTLED && this.settledStamp() == stamp ? stamp : UNSETTLED;
    }

    private void endVisibleListChange() {
        this.subset.clear();
        this.recorded.clear();
        this.endChange();
    }

    public static void beginWalk() {
        if (walking)
            throw new IllegalStateException("A visible section walk started inside another walk");

        walking = true;
    }

    public static void endWalk(ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections, ViewArea viewArea, @Nullable SectionUpdateTracker tracker) {
        if (!walking)
            throw new IllegalStateException("A visible section walk ended without starting");

        walking = false;

        int  previous   = generation;
        int  next       = nextGeneration(viewArea);
        long beStamp    = BLOCK_ENTITIES.settledStamp();
        long dirtyStamp = tracker == null ? UNSETTLED : DIRTY.settledStamp();
        int  carried    = discarded ? 0 : (BLOCK_ENTITIES.carriesFlags(beStamp) ? BLOCK_ENTITIES.flag : 0) | (DIRTY.carriesFlags(dirtyStamp) ? DIRTY.flag : 0);

        ObjectArrayList<SectionRenderDispatcher.RenderSection> blockEntities = BLOCK_ENTITIES.beginCarry();
        ObjectArrayList<SectionRenderDispatcher.RenderSection> dirty         = DIRTY.beginCarry();

        Object[] sections = visibleSections.elements();
        int      size     = visibleSections.size();

        for (int index = 0; index < size; index ++) {
            SectionRenderDispatcher.RenderSection section = (SectionRenderDispatcher.RenderSection) sections[index];
            SectionMark                           mark    = (SectionMark) section;
            int                                   value   = mark.txoptimizations$getSubsetMark();
            int                                   known   = value >>> FLAG_BITS == previous ? carried : 0;
            int                                   flags   = value & known;

            if ((known & BLOCK_ENTITIES.flag) == 0 && !section.getSectionMesh().getRenderableBlockEntities().isEmpty())
                flags |= BLOCK_ENTITIES.flag;

            if ((known & DIRTY.flag) == 0 && tracker != null && isDirty(tracker, section))
                flags |= DIRTY.flag;

            mark.txoptimizations$setSubsetMark(next << FLAG_BITS | flags);

            if ((flags & BLOCK_ENTITIES.flag) != 0)
                blockEntities.add(section);

            if ((flags & DIRTY.flag) != 0)
                dirty.add(section);
        }

        BLOCK_ENTITIES.endCarry(beStamp);
        DIRTY.endCarry(dirtyStamp);
        discarded = false;
    }

    public static void beginVisibleChange() {
        if (walking)
            return;

        discarded = true;
        DIRTY.beginChange();
        BLOCK_ENTITIES.beginChange();
    }

    public static void endVisibleChange() {
        if (walking)
            return;

        DIRTY.endVisibleListChange();
        BLOCK_ENTITIES.endVisibleListChange();
    }

    private static boolean isDirty(SectionUpdateTracker tracker, SectionRenderDispatcher.RenderSection section) {
        SectionUpdateTracker.SectionDirtyState state = SectionDirtyStates.of(tracker, section.index, section.getSectionNode());

        return state != null && state.isDirty();
    }

    private static int nextGeneration(ViewArea viewArea) {
        if (generation + 1 == GENERATIONS) {
            viewArea.sections.forEach(section -> ((SectionMark) section).txoptimizations$setSubsetMark(0));
            generation = 0;
        }

        generation ++;

        return generation;
    }
}
