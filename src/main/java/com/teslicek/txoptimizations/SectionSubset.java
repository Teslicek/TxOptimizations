package com.teslicek.txoptimizations;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;

public final class SectionSubset {

    public static final SectionSubset DIRTY = new SectionSubset();

    private static final long UNSETTLED = -1L;

    private final AtomicLong started  = new AtomicLong();
    private final AtomicLong finished = new AtomicLong();

    private ObjectArrayList<SectionRenderDispatcher.RenderSection> subset   = new ObjectArrayList<>();
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> recorded = new ObjectArrayList<>();
    private long                                                   stamp    = UNSETTLED;

    private SectionSubset() {
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
        long    finished = this.finished.get();
        long    started  = this.started.get();
        long    stamp    = finished == started ? started : UNSETTLED;
        boolean reuse    = stamp != UNSETTLED && stamp == this.stamp;

        ObjectArrayList<SectionRenderDispatcher.RenderSection> previous = this.recorded;

        this.recorded = this.subset;
        this.subset   = previous;
        this.stamp    = stamp;

        this.recorded.clear();

        if (reuse)
            return previous.iterator();

        return visibleSections.iterator();
    }

    public void record(SectionRenderDispatcher.RenderSection section) {
        this.recorded.add(section);
    }

    private void clear() {
        this.subset.clear();
        this.recorded.clear();
    }

    public static void beginVisibleChange() {
        DIRTY.beginChange();
    }

    public static void endVisibleChange() {
        DIRTY.clear();
        DIRTY.endChange();
    }
}
