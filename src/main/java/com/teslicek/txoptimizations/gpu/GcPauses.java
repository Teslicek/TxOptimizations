package com.teslicek.txoptimizations.gpu;

import com.sun.management.GarbageCollectionNotificationInfo;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

final class GcPauses {

    private static final List<Pause>                   PAUSES    = new ArrayList<>();
    private static final List<GarbageCollectorMXBean> COLLECTORS = ManagementFactory.getGarbageCollectorMXBeans();
    private static final NotificationListener          LISTENER  = GcPauses::onNotification;

    private static long jvmStartNanos;

    private GcPauses() {
    }

    static void start() {
        synchronized (PAUSES) {
            PAUSES.clear();
        }

        jvmStartNanos = System.nanoTime() - ManagementFactory.getRuntimeMXBean().getUptime() * 1_000_000L;

        for (GarbageCollectorMXBean collector : COLLECTORS)
            ((NotificationEmitter) collector).addNotificationListener(LISTENER, null, null);
    }

    static List<Pause> finish() {
        for (GarbageCollectorMXBean collector : COLLECTORS) {
            try {
                ((NotificationEmitter) collector).removeNotificationListener(LISTENER);
            } catch (javax.management.ListenerNotFoundException exception) {
                throw new IllegalStateException("GC listener was not registered on " + collector.getName(), exception);
            }
        }

        synchronized (PAUSES) {
            return List.copyOf(PAUSES);
        }
    }

    private static void onNotification(Notification notification, Object handback) {
        if (!notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION))
            return;

        GarbageCollectionNotificationInfo info  = GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
        long                              start = jvmStartNanos + info.getGcInfo().getStartTime() * 1_000_000L;
        long                              end   = jvmStartNanos + info.getGcInfo().getEndTime() * 1_000_000L;

        synchronized (PAUSES) {
            PAUSES.add(new Pause(info.getGcName() + " (" + info.getGcCause() + ")", start, end));
        }
    }

    record Pause(String name, long startNanos, long endNanos) {
    }
}
