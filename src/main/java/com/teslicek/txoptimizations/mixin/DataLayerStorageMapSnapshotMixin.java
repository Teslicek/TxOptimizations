package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.LightSectionTree;
import com.teslicek.txoptimizations.LightSnapshotMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DataLayerStorageMap.class)
public abstract class DataLayerStorageMapSnapshotMixin implements LightSnapshotMap {

    @Shadow
    @Final
    protected Long2ObjectOpenHashMap<DataLayer> map;

    @Override
    public Long2ObjectOpenHashMap<DataLayer> txoptimizations$sections() {
        return this.map;
    }

    @Override
    public int txoptimizations$top(long zeroNode) {
        throw new IllegalStateException("No light snapshot support for " + this.getClass().getName());
    }

    @Override
    public int txoptimizations$skyLight(long blockNode) {
        throw new IllegalStateException("No sky light in " + this.getClass().getName());
    }

    @Override
    public DataLayerStorageMap<?> txoptimizations$snapshotWith(LightSectionTree tree) {
        throw new IllegalStateException("No light snapshot support for " + this.getClass().getName());
    }
}
