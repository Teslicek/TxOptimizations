package com.teslicek.txoptimizations;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import net.minecraft.world.level.chunk.DataLayer;

public final class LightSectionView extends Long2ObjectOpenHashMap<DataLayer> {

    private final LightSectionTree tree;

    public LightSectionView(LightSectionTree tree) {
        super(0);
        this.tree = tree;
    }

    @Override
    public DataLayer get(long key) {
        return this.tree.layer(key);
    }

    @Override
    public boolean containsKey(long key) {
        return this.tree.layer(key) != null;
    }

    @Override
    public DataLayer put(long key, DataLayer value) {
        throw new UnsupportedOperationException("Light snapshots are read-only");
    }

    @Override
    public DataLayer remove(long key) {
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
    public Long2ObjectOpenHashMap<DataLayer> clone() {
        throw new UnsupportedOperationException("Light snapshots only support lookups");
    }

    @Override
    public Long2ObjectMap.FastEntrySet<DataLayer> long2ObjectEntrySet() {
        throw new UnsupportedOperationException("Light snapshots only support lookups");
    }

    @Override
    public LongSet keySet() {
        throw new UnsupportedOperationException("Light snapshots only support lookups");
    }

    @Override
    public ObjectCollection<DataLayer> values() {
        throw new UnsupportedOperationException("Light snapshots only support lookups");
    }
}
