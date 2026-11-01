package com.teslicek.txoptimizations;

import org.lwjgl.system.MemoryStack;

public final class RenderThreadStack {

    private static volatile Thread      thread;
    private static volatile MemoryStack stack;

    private RenderThreadStack() {
    }

    public static void capture(MemoryStack captured) {
        if (thread != null)
            throw new IllegalStateException("The render thread memory stack is already captured");

        stack  = captured;
        thread = Thread.currentThread();
    }

    public static void handTo(Thread owner, MemoryStack ownerStack) {
        stack  = ownerStack;
        thread = owner;
    }

    public static Thread owner() {
        return thread;
    }

    public static boolean isOwner() {
        return Thread.currentThread() == thread;
    }

    public static MemoryStack stack() {
        return stack;
    }
}
