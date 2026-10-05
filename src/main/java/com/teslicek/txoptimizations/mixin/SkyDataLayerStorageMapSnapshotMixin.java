package com.teslicek.txoptimizations.mixin;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import net.minecraft.world.level.lighting.SkyLightSectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SkyLightSectionStorage.SkyDataLayerStorageMap.class)
public abstract class SkyDataLayerStorageMapSnapshotMixin {

    @Shadow
    private int currentLowestY;

    @Shadow
    @Final
    private Long2IntOpenHashMap topSections;

    public DataLayerStorageMap<?> txoptimizations$snapshotWith(Long2ObjectOpenHashMap<DataLayer> sections) {
        return new SkyLightSectionStorage.SkyDataLayerStorageMap(sections, this.topSections.clone(), this.currentLowestY);
    }
}
