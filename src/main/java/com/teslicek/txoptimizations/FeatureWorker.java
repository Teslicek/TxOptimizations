package com.teslicek.txoptimizations;

import com.teslicek.txoptimizations.mixin.FeaturePreparedGroupAccessor;
import com.teslicek.txoptimizations.mixin.StagedVertexBufferListAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRendererMap;
import net.minecraft.client.renderer.feature.submit.SubmitNode;

final class FeatureWorker {

    private static final int  INITIAL_CAPACITY = 1 << 20;
    private static final int  WINDOW_FRAMES    = 64;
    private static final int  PROBE_FRAMES     = 256;
    private static final int  MAX_PROBE_FRAMES = 16384;
    private static final long MAX_INLINE_NANOS = 1_000_000_000L;
    private static final long OVERHEAD_NANOS   = 3_000L;

    private final AtomicReference<Runnable> job    = new AtomicReference<>();
    private final List<Object>              groups = new ArrayList<>();
    private final StagedVertexBuffer        shard;
    private final Thread                    thread;
    private volatile boolean                done   = true;
    private volatile Throwable              failure;
    private volatile long                   jobNanos;
    private int                             windowFrames;
    private long                            windowSavedNanos;
    private int                             inlineFrames;
    private int                             probeFrames = PROBE_FRAMES;
    private long                            inlineSince;
    private boolean                         inline;
    private boolean                         probing;

    FeatureWorker(String name) {
        this.shard  = new StagedVertexBuffer((Supplier<String>) () -> name, INITIAL_CAPACITY);
        this.thread = Thread.ofPlatform().name(name).daemon().priority(Thread.MAX_PRIORITY).start(this::work);
    }

    List<Object> groups() {
        return this.groups;
    }

    boolean isCurrentThread() {
        return Thread.currentThread() == this.thread;
    }

    boolean claimFrame() {
        if (!this.inline)
            return true;

        this.inlineFrames ++;

        if (this.inlineFrames < PROBE_FRAMES || this.inlineFrames < this.probeFrames && System.nanoTime() - this.inlineSince < MAX_INLINE_NANOS)
            return false;

        this.inline       = false;
        this.probing      = true;
        this.inlineFrames = 0;

        return true;
    }

    void start(FeatureFrameContext context, FeatureRendererMap renderers, List<SubmitNode> submits) {
        if (this.groups.isEmpty())
            return;

        FeatureFrameContext worker = new FeatureFrameContext(context.options(), context.font(), context.blockStateModelSet(), context.blockColors(), context.textureManager(), context.atlasManager(), context.lightmap(), this.shard);

        this.done    = false;
        this.failure = null;
        this.job.set(() -> this.prepare(worker, renderers, submits));

        if (!PresentThread.wakeWhileSpinning(this.thread))
            LockSupport.unpark(this.thread);
    }

    void finish(StagedVertexBuffer main) {
        if (this.groups.isEmpty())
            return;

        long     waitStart = System.nanoTime();
        Runnable pending   = this.job.getAndSet(null);

        if (pending != null)
            this.run(pending);

        while (!this.done)
            Thread.onSpinWait();

        this.recordFrame(System.nanoTime() - waitStart, this.jobNanos);

        this.groups.clear();

        Throwable thrown = this.failure;

        if (thrown != null) {
            this.failure = null;

            throw new IllegalStateException("Preparing features on " + this.thread.getName() + " failed", thrown);
        }

        List<StagedVertexBuffer.Draw> shardDraws = ((StagedVertexBufferListAccessor) this.shard).txoptimizations$draws();
        List<StagedVertexBuffer.Draw> mainDraws  = ((StagedVertexBufferListAccessor) main).txoptimizations$draws();

        for (StagedVertexBuffer.Draw draw : shardDraws) {
            mainDraws.add(draw);
            main.requestIndexCount(draw);
        }

        shardDraws.clear();
    }

    private void recordFrame(long waitNanos, long workNanos) {
        this.windowFrames ++;
        this.windowSavedNanos += Math.max(0L, workNanos - waitNanos);

        if (this.windowFrames < WINDOW_FRAMES)
            return;

        boolean idle = this.windowSavedNanos < OVERHEAD_NANOS * WINDOW_FRAMES;

        this.probeFrames      = idle && this.probing ? Math.min(this.probeFrames * 2, MAX_PROBE_FRAMES) : PROBE_FRAMES;
        this.probing          = false;
        this.inline           = idle;
        this.inlineSince      = System.nanoTime();
        this.windowFrames     = 0;
        this.windowSavedNanos = 0L;
    }

    private void prepare(FeatureFrameContext context, FeatureRendererMap renderers, List<SubmitNode> submits) {
        for (Object group : this.groups)
            ((FeaturePreparedGroupAccessor) group).txoptimizations$prepare(context, renderers, submits);

        ((StagedVertexBufferListAccessor) this.shard).txoptimizations$finishLastVertexBuilder();
    }

    private void work() {
        while (true) {
            Runnable next = this.job.getAndSet(null);

            if (next == null) {
                LockSupport.park();
                continue;
            }

            this.run(next);
        }
    }

    private void run(Runnable next) {
        long start = System.nanoTime();

        try {
            next.run();
        } catch (Throwable thrown) {
            this.failure = thrown;
        } finally {
            this.jobNanos = System.nanoTime() - start;
            this.done     = true;
        }
    }
}
