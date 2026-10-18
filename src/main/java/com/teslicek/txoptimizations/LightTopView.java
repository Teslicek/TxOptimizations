package com.teslicek.txoptimizations;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.SectionPos;

public final class LightTopView extends Long2IntOpenHashMap {

    private final LightSectionTree tree;

    public LightTopView(LightSectionTree tree) {
        super(0);
        this.tree = tree;
    }

    @Override
    public int get(long key) {
        if (SectionPos.y(key) != 0)
            return this.defRetValue;

        int top = this.tree.top(key);

        return top == LightSectionTree.NO_TOP ? this.defRetValue : top;
    }

    @Override
    public boolean containsKey(long key) {
        return SectionPos.y(key) == 0 && this.tree.top(key) != LightSectionTree.NO_TOP;
    }

    @Override
    public int put(long key, int value) {
        throw new UnsupportedOperationException("Light snapshots are read-only");
    }

    @Override
    public int remove(long key) {
        throw new UnsupportedOperationException("Light snapshots are read-only");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Light snapshots are read-only");
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("Light snapshots only support lookups");
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("Light snapshots only support lookups");
    }

    @Override
    public Long2IntOpenHashMap clone() {
        throw new UnsupportedOperationException("Light snapshots only support lookups");
    }

    @Override
    public Long2IntMap.FastEntrySet long2IntEntrySet() {
        throw new UnsupportedOperationException("Light snapshots only support lookups");
    }

    @Override
    public LongSet keySet() {
        throw new UnsupportedOperationException("Light snapshots only support lookups");
    }

    @Override
    public IntCollection values() {
        throw new UnsupportedOperationException("Light snapshots only support lookups");
    }
}
