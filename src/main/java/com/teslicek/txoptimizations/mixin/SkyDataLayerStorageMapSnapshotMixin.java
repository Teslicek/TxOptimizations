package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.LightSectionTree;
import com.teslicek.txoptimizations.LightSectionView;
import com.teslicek.txoptimizations.LightTopView;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
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

    public int txoptimizations$top(long zeroNode) {
        return this.topSections.getOrDefault(zeroNode, LightSectionTree.NO_TOP);
    }

    public DataLayerStorageMap<?> txoptimizations$snapshotWith(LightSectionTree tree) {
        return new SkyLightSectionStorage.SkyDataLayerStorageMap(new LightSectionView(tree), new LightTopView(tree), this.currentLowestY);
    }
}
