package com.teslicek.txoptimizations;

public final class RecyclePolling {

    private static boolean active;
    private static long pendingFrame = -1L;
    private static long lowestPendingSubmit;

    private RecyclePolling() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean active) {
        RecyclePolling.active = active;
    }

    public static boolean isKnownPending(long submitIndex) {
        return pendingFrame == ClientClock.frame() && submitIndex >= lowestPendingSubmit;
    }

    public static void markPending(long submitIndex) {
        long frame = ClientClock.frame();

        if (pendingFrame != frame) {
            pendingFrame        = frame;
            lowestPendingSubmit = submitIndex;
            return;
        }

        lowestPendingSubmit = Math.min(lowestPendingSubmit, submitIndex);
    }
}
