package com.teslicek.txoptimizations.cull;

import net.caffeinemc.mods.sodium.client.util.iterator.ByteIterator;

final class FilteredSectionIterator implements ByteIterator {

    private static final int CAPACITY = 256;

    private final byte[] sections = new byte[CAPACITY];
    private int          count;
    private int          position;

    void reset() {
        this.count    = 0;
        this.position = 0;
    }

    void add(int section) {
        this.sections[this.count ++] = (byte) section;
    }

    boolean isEmpty() {
        return this.count == 0;
    }

    @Override
    public boolean hasNext() {
        return this.position < this.count;
    }

    @Override
    public int nextByteAsInt() {
        return this.sections[this.position ++] & 0xFF;
    }
}
