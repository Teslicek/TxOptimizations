package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.LightSectionTree;
import com.teslicek.txoptimizations.LightSectionView;
import net.minecraft.world.level.lighting.BlockLightSectionStorage;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockLightSectionStorage.BlockDataLayerStorageMap.class)
public abstract class BlockDataLayerStorageMapSnapshotMixin {

    public int txoptimizations$top(long zeroNode) {
        return LightSectionTree.NO_TOP;
    }

    public DataLayerStorageMap<?> txoptimizations$snapshotWith(LightSectionTree tree) {
        return new BlockLightSectionStorage.BlockDataLayerStorageMap(new LightSectionView(tree));
    }
}
