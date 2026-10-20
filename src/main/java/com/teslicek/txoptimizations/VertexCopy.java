package com.teslicek.txoptimizations;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import org.lwjgl.system.MemoryUtil;

public final class VertexCopy {

    private static final int CHUNK_BYTES = 32768;
    private static final int IDLE        = 0;
    private static final int QUEUED      = 1;
    private static final int RUNNING     = 2;

    private final AtomicInteger next     = new AtomicInteger();
    private final AtomicInteger state    = new AtomicInteger();
    private final AtomicInteger measured = new AtomicInteger();

    private volatile Throwable failure;
    private long[]             sources       = new long[64];
    private long[]             targets       = new long[64];
    private int[]              sizes         = new int[64];
    private long[]             boundsSources = new long[8];
    private int[]              boundsCounts  = new int[8];
    private int[]              boundsStrides = new int[8];
    private float[][]          boundsBoxes   = new float[8][];
    private int[]              boundsOffsets = new int[8];
    private int                count;
    private int                bounds;
    private boolean            pending;
    private boolean            background;

    public void begin() {
        if (this.pending)
            throw new IllegalStateException("The previous vertex copies have not finished");

        this.count  = 0;
        this.bounds = 0;
    }

    public void addBounds(long source, int vertices, int stride, float[] boxes, int offset) {
        if (this.bounds == this.boundsCounts.length) {
            this.boundsSources = Arrays.copyOf(this.boundsSources, this.bounds * 2);
            this.boundsCounts  = Arrays.copyOf(this.boundsCounts, this.bounds * 2);
            this.boundsStrides = Arrays.copyOf(this.boundsStrides, this.bounds * 2);
            this.boundsBoxes   = Arrays.copyOf(this.boundsBoxes, this.bounds * 2);
            this.boundsOffsets = Arrays.copyOf(this.boundsOffsets, this.bounds * 2);
        }

        this.boundsSources[this.bounds] = source;
        this.boundsCounts[this.bounds]  = vertices;
        this.boundsStrides[this.bounds] = stride;
        this.boundsBoxes[this.bounds]   = boxes;
        this.boundsOffsets[this.bounds] = offset;
        this.bounds ++;
    }

    public void add(long source, long target, int size) {
        for (int offset = 0; offset < size; offset += CHUNK_BYTES) {
            if (this.count == this.sizes.length) {
                this.sources = Arrays.copyOf(this.sources, this.count * 2);
                this.targets = Arrays.copyOf(this.targets, this.count * 2);
                this.sizes   = Arrays.copyOf(this.sizes, this.count * 2);
            }

            this.sources[this.count] = source + offset;
            this.targets[this.count] = target + offset;
            this.sizes[this.count]   = Math.min(CHUNK_BYTES, size - offset);
            this.count ++;
        }
    }

    public void seal() {
        if (this.pending)
            throw new IllegalStateException("The previous vertex copies have not finished");

        if (this.count == 0 && this.bounds == 0)
            return;

        this.next.set(0);
        this.measured.set(0);
        this.pending = true;
    }

    public void startInBackground() {
        if (!this.pending)
            return;

        if (this.state.get() != IDLE)
            throw new IllegalStateException("The vertex copies were already started");

        this.background = true;
        this.state.set(QUEUED);
        Worker.queue(this);
    }

    public void finish() {
        if (!this.pending)
            return;

        this.state.compareAndSet(QUEUED, IDLE);
        this.copyClaimed();

        while (this.state.get() == RUNNING)
            Thread.onSpinWait();

        this.pending    = false;
        this.background = false;
        Arrays.fill(this.boundsBoxes, 0, this.bounds, null);

        Throwable thrown = this.failure;

        if (thrown == null)
            return;

        this.failure = null;

        throw new IllegalStateException("Copying vertices on the vertex copy thread failed", thrown);
    }

    public boolean isInBackground() {
        return this.background;
    }

    public void finishBounds() {
        if (!this.pending)
            return;

        int bounds = this.bounds;
        int jobs   = bounds + this.count;

        for (int job = this.next.getAndIncrement(); job < jobs; job = this.next.getAndIncrement()) {
            this.run(job, bounds);

            if (job >= bounds)
                break;
        }

        while (this.measured.get() < bounds) {
            Throwable thrown = this.failure;

            if (thrown != null)
                throw new IllegalStateException("Measuring outline bounds on the vertex copy thread failed", thrown);

            Thread.onSpinWait();
        }
    }

    public static void runQueuedCopy() {
        Worker.runQueuedCopy();
    }

    private void runQueued() {
        if (!this.state.compareAndSet(QUEUED, RUNNING))
            return;

        try {
            this.copyClaimed();
        } catch (Throwable thrown) {
            this.failure = thrown;
        } finally {
            this.state.set(IDLE);
        }
    }

    private void copyClaimed() {
        int bounds = this.bounds;
        int jobs   = bounds + this.count;

        for (int job = this.next.getAndIncrement(); job < jobs; job = this.next.getAndIncrement())
            this.run(job, bounds);
    }

    private void run(int job, int bounds) {
        if (job >= bounds) {
            MemoryUtil.memCopy(this.sources[job - bounds], this.targets[job - bounds], this.sizes[job - bounds]);
            return;
        }

        PositionBounds.measure(this.boundsSources[job], this.boundsCounts[job], this.boundsStrides[job], this.boundsBoxes[job], this.boundsOffsets[job]);
        this.measured.incrementAndGet();
    }

    private static final class Worker {

        private static final AtomicReference<VertexCopy> JOB    = new AtomicReference<>();
        private static final Thread                      THREAD = Thread.ofPlatform().name("TxOptimizations Vertex Copy").daemon().priority(Thread.MAX_PRIORITY).start(Worker::work);

        private static void queue(VertexCopy copy) {
            JOB.set(copy);

            if (!FrameSpin.isFastFrames())
                LockSupport.unpark(THREAD);
        }

        private static void runQueuedCopy() {
            if (JOB.get() == null)
                return;

            VertexCopy copy = JOB.getAndSet(null);

            if (copy != null)
                copy.runQueued();
        }

        private static void work() {
            while (true) {
                if (JOB.get() == null) {
                    LockSupport.park(Worker.class);
                    continue;
                }

                runQueuedCopy();
            }
        }
    }
}
