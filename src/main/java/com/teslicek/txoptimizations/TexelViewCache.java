package com.teslicek.txoptimizations;

public interface TexelViewCache {

    long txoptimizations$findTexelView(long offset, long range, int format);

    boolean txoptimizations$storeTexelView(long offset, long range, int format, long view);
}
