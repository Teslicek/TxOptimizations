package com.teslicek.txoptimizations;

import org.lwjgl.system.MemoryStack;

public final class RenderThreadStack {

    private static Thread      thread;
    private static MemoryStack stack;

    private RenderThreadStack() {
    }

    public static void capture(MemoryStack captured) {
        if (thread != null)
            throw new IllegalStateException("The render thread memory stack is already captured");

        stack  = captured;
        thread = Thread.currentThread();
    }

    public static boolean isOwner() {
        return Thread.currentThread() == thread;
    }

    public static MemoryStack stack() {
        return stack;
    }
}
