package com.teslicek.txoptimizations;

public final class PendingSections {

    private static int count;

    private PendingSections() {
    }

    public static int count() {
        return count;
    }

    public static void add() {
        count ++;
    }

    public static void remove() {
        if (count == 0)
            throw new IllegalStateException("Pending section count went below zero");

        count --;
    }
}
