package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.LightSnapshotMap;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.SkyLightSectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(SkyLightSectionStorage.class)
public abstract class SkyLightSectionStorageLookupMixin extends LayerLightSectionStorage<SkyLightSectionStorage.SkyDataLayerStorageMap> {

    protected SkyLightSectionStorageLookupMixin(LightLayer layer, LightChunkGetter chunkSource, SkyLightSectionStorage.SkyDataLayerStorageMap initialMap) {
        super(layer, chunkSource, initialMap);
    }

    @Overwrite
    protected int getLightValue(long blockNode) {
        return ((LightSnapshotMap) (Object) this.visibleSectionData).txoptimizations$skyLight(blockNode);
    }
}
