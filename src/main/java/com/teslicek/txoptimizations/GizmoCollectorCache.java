package com.teslicek.txoptimizations;

import net.minecraft.gizmos.GizmoCollector;
import org.jspecify.annotations.Nullable;

public final class GizmoCollectorCache {

    private static @Nullable GizmoCollector mainThreadCollector;

    private GizmoCollectorCache() {
    }

    public static Object get(ThreadLocal<GizmoCollector> collector) {
        if (RenderThreadStack.isMain())
            return mainThreadCollector;

        return collector.get();
    }

    public static void set(ThreadLocal<GizmoCollector> collector, Object value) {
        if (RenderThreadStack.isMain()) {
            mainThreadCollector = (GizmoCollector) value;
            return;
        }

        collector.set((GizmoCollector) value);
    }
}
