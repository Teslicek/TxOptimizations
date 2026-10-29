package com.teslicek.txoptimizations.hud;

public final class HudLayerTimer {

    private static final int SAMPLES = 5;

    private final long[] samples = new long[SAMPLES];
    private boolean      cached;
    private int          pass    = 1;
    private int          sampleIndex;
    private long         start;
    private int          position;
    private int          minX    = Integer.MAX_VALUE;
    private int          minY    = Integer.MAX_VALUE;
    private int          maxX    = Integer.MIN_VALUE;
    private int          maxY    = Integer.MIN_VALUE;

    HudLayerTimer(boolean cached) {
        this.cached = cached;
    }

    public boolean isCached() {
        return this.cached;
    }

    public boolean shouldRender(int currentPass) {
        return !this.cached || this.pass == currentPass;
    }

    void uncache() {
        this.cached = false;
    }

    void place(int position) {
        this.position = position;
    }

    boolean include(int left, int top, int right, int bottom) {
        if (left >= this.minX && top >= this.minY && right <= this.maxX && bottom <= this.maxY)
            return false;

        this.minX = Math.min(this.minX, left);
        this.minY = Math.min(this.minY, top);
        this.maxX = Math.max(this.maxX, right);
        this.maxY = Math.max(this.maxY, bottom);

        return true;
    }

    boolean drawsBelow(HudLayerTimer above) {
        return this.position < above.position && this.minX < above.maxX && above.minX < this.maxX && this.minY < above.maxY && above.minY < this.maxY;
    }

    void setPass(int pass) {
        this.pass = pass;
    }

    long totalTime() {
        long total = 0L;

        for (long sample : this.samples)
            total += sample;

        return total;
    }

    void begin() {
        this.start = System.nanoTime();
    }

    void end() {
        this.samples[this.sampleIndex] = System.nanoTime() - this.start;
        this.sampleIndex               = (this.sampleIndex + 1) % SAMPLES;
    }
}
