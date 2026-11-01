package com.teslicek.txoptimizations;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;

public final class VisibleSectionBuffers {

    private static final int VISIBLE_CAPACITY = 10000;
    private static final int NEARBY_CAPACITY  = 50;

    @SuppressWarnings("unchecked")
    private static final ObjectArrayList<SectionRenderDispatcher.RenderSection>[] VISIBLE = new ObjectArrayList[2];
    @SuppressWarnings("unchecked")
    private static final ObjectArrayList<SectionRenderDispatcher.RenderSection>[] NEARBY  = new ObjectArrayList[2];
    private static final int[]                                                    STAMPS  = new int[2];

    private static int          extractIndex;
    private static volatile int renderIndex;
    private static int          refills;

    private VisibleSectionBuffers() {
    }

    public static void adopt(ObjectArrayList<SectionRenderDispatcher.RenderSection> visible, ObjectArrayList<SectionRenderDispatcher.RenderSection> nearby) {
        if (VISIBLE[0] != null)
            throw new IllegalStateException("The visible section lists were adopted twice");

        VISIBLE[0] = visible;
        NEARBY[0]  = nearby;
        VISIBLE[1] = new ObjectArrayList<>(VISIBLE_CAPACITY);
        NEARBY[1]  = new ObjectArrayList<>(NEARBY_CAPACITY);
    }

    public static ObjectArrayList<SectionRenderDispatcher.RenderSection> visible() {
        return VISIBLE[index()];
    }

    public static ObjectArrayList<SectionRenderDispatcher.RenderSection> nearby() {
        return NEARBY[index()];
    }

    public static int stamp() {
        return STAMPS[index()];
    }

    public static void beginRefill() {
        if (RenderWorker.isCurrent())
            throw new IllegalStateException("Visible sections were refilled on the render worker");

        int target = 1 - renderIndex;

        extractIndex   = target;
        STAMPS[target] = ++ refills;
        VISIBLE[target].clear();
        NEARBY[target].clear();
    }

    public static void handOff() {
        renderIndex = extractIndex;
    }

    public static int renderIndex() {
        return renderIndex;
    }

    public static ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleAt(int index) {
        return VISIBLE[index];
    }

    public static ObjectArrayList<SectionRenderDispatcher.RenderSection> nearbyAt(int index) {
        return NEARBY[index];
    }

    private static int index() {
        return RenderWorker.isCurrent() ? renderIndex : extractIndex;
    }
}
