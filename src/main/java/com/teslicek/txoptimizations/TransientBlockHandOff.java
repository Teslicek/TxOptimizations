package com.teslicek.txoptimizations;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.state.level.TransientBlockRenderState;

public final class TransientBlockHandOff {

    @SuppressWarnings("unchecked")
    private static final List<Pending>[] PENDING = new List[] {new ArrayList<Pending>(), new ArrayList<Pending>()};

    private TransientBlockHandOff() {
    }

    public static void add(long blockNode, TransientBlockRenderState state) {
        if (RenderWorker.isCurrent())
            throw new IllegalStateException("A transient block was added on the render worker");

        PENDING[RenderStateBuffers.parity()].add(new Pending(blockNode, state));
    }

    public static List<Pending> drainFor(int parity) {
        return PENDING[parity];
    }

    public record Pending(long blockNode, TransientBlockRenderState state) {
    }
}
