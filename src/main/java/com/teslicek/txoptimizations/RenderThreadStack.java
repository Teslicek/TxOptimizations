package com.teslicek.txoptimizations;

import org.lwjgl.system.MemoryStack;

public final class RenderThreadStack {

    private static volatile Thread      thread;
    private static volatile MemoryStack stack;

    private RenderThreadStack() {
    }

    public static void ensureCaptured() {
        if (thread == null) {
            stack  = MemoryStack.stackGet();
            thread = Thread.currentThread();

            return;
        }

        if (thread != Thread.currentThread() && !RenderWorker.isCurrent())
            throw new IllegalStateException("The render thread memory stack was captured on " + thread.getName() + ", not " + Thread.currentThread().getName());
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
