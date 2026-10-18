package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.LightSectionTree;
import com.teslicek.txoptimizations.LightSnapshotMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LayerLightSectionStorage.class)
public abstract class LayerLightSectionStorageSnapshotMixin {

    @Shadow
    @Final
    protected LongSet changedSections;

    @Unique
    private LightSectionTree txoptimizations$tree;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$createTree(LightLayer layer, LightChunkGetter chunkSource, DataLayerStorageMap<?> initialMap, CallbackInfo ci) {
        BlockGetter level = chunkSource.getLevel();

        this.txoptimizations$tree = new LightSectionTree(level.getMinSectionY() - LevelLightEngine.LIGHT_SECTION_PADDING, level.getSectionsCount() + 2 * LevelLightEngine.LIGHT_SECTION_PADDING);
    }

    @Redirect(method = "swapSectionMap", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/lighting/DataLayerStorageMap;copy()Lnet/minecraft/world/level/lighting/DataLayerStorageMap;"))
    private DataLayerStorageMap<?> txoptimizations$snapshot(DataLayerStorageMap<?> updating) {
        LightSnapshotMap source = (LightSnapshotMap) updating;

        this.txoptimizations$tree = this.txoptimizations$tree.with(this.changedSections, source);

        return source.txoptimizations$snapshotWith(this.txoptimizations$tree);
    }
}
