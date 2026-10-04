package com.teslicek.txoptimizations;

import org.lwjgl.system.MemoryStack;

public final class RenderThreadStack {

    private static Owner owner;

    private RenderThreadStack() {
    }

    public static void capture(MemoryStack stack) {
        if (owner != null)
            throw new IllegalStateException("The render thread memory stack is already captured");

        owner = new Owner(Thread.currentThread(), stack);
    }

    public static Owner owner() {
        return owner;
    }

    public record Owner(Thread thread, MemoryStack stack) {
    }
}
