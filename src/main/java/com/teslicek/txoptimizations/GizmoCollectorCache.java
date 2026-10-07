package com.teslicek.txoptimizations;

import net.minecraft.gizmos.GizmoCollector;

public final class GizmoCollectorCache {

    private static volatile Entry cached;

    private GizmoCollectorCache() {
    }

    public static Object get(ThreadLocal<GizmoCollector> collector) {
        Entry entry = cached;

        if (entry != null && entry.thread() == Thread.currentThread())
            return entry.collector();

        return collector.get();
    }

    public static void set(ThreadLocal<GizmoCollector> collector, Object value) {
        GizmoCollector gizmoCollector = (GizmoCollector) value;

        collector.set(gizmoCollector);
        cached = new Entry(Thread.currentThread(), gizmoCollector);
    }

    private record Entry(Thread thread, GizmoCollector collector) {
    }
}
