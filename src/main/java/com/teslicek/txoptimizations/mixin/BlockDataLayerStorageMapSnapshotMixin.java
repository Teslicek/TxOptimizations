package com.teslicek.txoptimizations.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.BlockLightSectionStorage;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockLightSectionStorage.BlockDataLayerStorageMap.class)
public abstract class BlockDataLayerStorageMapSnapshotMixin {

    public DataLayerStorageMap<?> txoptimizations$snapshotWith(Long2ObjectOpenHashMap<DataLayer> sections) {
        return new BlockLightSectionStorage.BlockDataLayerStorageMap(sections);
    }
}
