package com.teslicek.txoptimizations.hud;

public final class HudLayerTimer {

    private static final int SAMPLES = 5;

    private final long[] samples = new long[SAMPLES];
    private boolean      cached;
    private int          pass    = 1;
    private int          sampleIndex;
    private long         start;

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
