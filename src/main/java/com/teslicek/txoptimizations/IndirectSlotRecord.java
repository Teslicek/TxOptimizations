package com.teslicek.txoptimizations;

import java.util.Arrays;

public final class IndirectSlotRecord {

    private Object[] batches  = new Object[64];
    private int[]    versions = new int[64];
    private int      count;

    public boolean matches(int index, Object batch, int version) {
        return index < this.count && this.batches[index] == batch && this.versions[index] == version;
    }

    public void store(int index, Object batch, int version) {
        if (index == this.batches.length) {
            this.batches  = Arrays.copyOf(this.batches, index * 2);
            this.versions = Arrays.copyOf(this.versions, index * 2);
        }

        this.batches[index]  = batch;
        this.versions[index] = version;
        this.count           = index + 1;
    }

    public void setCount(int count) {
        if (count > this.count)
            throw new IllegalStateException("Indirect slot record has " + this.count + " commands, asked to keep " + count);

        Arrays.fill(this.batches, count, this.count, null);
        this.count = count;
    }
}
