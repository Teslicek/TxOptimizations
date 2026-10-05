package com.teslicek.txoptimizations;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import net.minecraft.world.level.chunk.DataLayer;

public final class LayeredSectionMap extends Long2ObjectOpenHashMap<DataLayer> {

    private final Long2ObjectOpenHashMap<DataLayer> base;
    private final Long2ObjectOpenHashMap<DataLayer> changes;

    public LayeredSectionMap(Long2ObjectOpenHashMap<DataLayer> base, Long2ObjectOpenHashMap<DataLayer> changes) {
        super(0);
        this.base    = base;
        this.changes = changes;
    }

    public Long2ObjectOpenHashMap<DataLayer> base() {
        return this.base;
    }

    @Override
    public DataLayer get(long key) {
        DataLayer changed = this.changes.get(key);

        if (changed != null)
            return changed;

        if (this.changes.containsKey(key))
            return null;

        return this.base.get(key);
    }

    @Override
    public boolean containsKey(long key) {
        if (this.changes.get(key) != null)
            return true;

        if (this.changes.containsKey(key))
            return false;

        return this.base.containsKey(key);
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
