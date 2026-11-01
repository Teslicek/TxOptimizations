package com.teslicek.txoptimizations;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class FrameQueue<T> {

    @SuppressWarnings("unchecked")
    private final List<T>[] pending = new List[] {new ArrayList<T>(), new ArrayList<T>()};
    private final String    name;

    public FrameQueue(String name) {
        this.name = name;
    }

    public void add(T value) {
        if (RenderWorker.isCurrent())
            throw new IllegalStateException(this.name + " were queued on the render worker");

        this.pending[RenderStateBuffers.parity()].add(value);
    }

    public void drain(Consumer<T> consumer) {
        if (!RenderWorker.isCurrent())
            throw new IllegalStateException(this.name + " were drained outside the render worker");

        List<T> frame = this.pending[RenderStateBuffers.renderParity()];

        for (T value : frame)
            consumer.accept(value);

        frame.clear();
    }
}
