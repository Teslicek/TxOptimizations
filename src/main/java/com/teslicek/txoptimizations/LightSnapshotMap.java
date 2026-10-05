package com.teslicek.txoptimizations;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.DataLayerStorageMap;

public interface LightSnapshotMap {

    Long2ObjectOpenHashMap<DataLayer> txoptimizations$sections();

    DataLayerStorageMap<?> txoptimizations$snapshotWith(Long2ObjectOpenHashMap<DataLayer> sections);
}
