package com.teslicek.txoptimizations;

import org.lwjgl.system.MemoryStack;

public final class RenderThreadStack {

    private static volatile Thread      main;
    private static volatile MemoryStack mainStack;

    private RenderThreadStack() {
    }

    public static void ensureCaptured() {
        if (main == null) {
            mainStack = MemoryStack.stackGet();
            main      = Thread.currentThread();

            return;
        }

        if (main != Thread.currentThread())
            throw new IllegalStateException("The main thread was captured as " + main.getName() + ", not " + Thread.currentThread().getName());
    }

    public static boolean isMain() {
        return Thread.currentThread() == main;
    }

    public static boolean isOwner() {
        return RenderWorker.isCurrent();
    }

    public static MemoryStack mainStack() {
        return mainStack;
    }
}
