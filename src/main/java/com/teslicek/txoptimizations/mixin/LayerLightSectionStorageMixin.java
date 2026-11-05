package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.BlockEntityLight;
import com.teslicek.txoptimizations.LightSectionTree;
import com.teslicek.txoptimizations.LightSnapshotMap;
import com.teslicek.txoptimizations.SectionNeighbors;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.chunk.DataLayer;
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
public abstract class LayerLightSectionStorageMixin {

    @Shadow
    @Final
    protected Long2ObjectMap<DataLayer> queuedSections;

    @Unique
    private Long2ObjectOpenHashMap<DataLayer> txoptimizations$queuedTable;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMaps;synchronize(Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;)Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;"))
    private Long2ObjectMap<DataLayer> txoptimizations$keepQueuedTable(Long2ObjectMap<DataLayer> sections) {
        this.txoptimizations$queuedTable = (Long2ObjectOpenHashMap<DataLayer>) sections;

        return Long2ObjectMaps.synchronize(sections);
    }

    @Redirect(method = "markNewInconsistencies", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMaps;fastIterator(Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;)Lit/unimi/dsi/fastutil/objects/ObjectIterator;"))
    private ObjectIterator<Long2ObjectMap.Entry<DataLayer>> txoptimizations$queuedIterator(Long2ObjectMap<DataLayer> sections) {
        if (sections != this.queuedSections)
            throw new IllegalStateException("Unexpected light section map " + sections.getClass().getName());

        return this.txoptimizations$queuedTable.long2ObjectEntrySet().fastIterator();
    }

    @Shadow
    @Final
    protected LightChunkGetter chunkSource;

    @Shadow
    @Final
    protected LongSet changedSections;

    @Unique
    private LightSectionTree txoptimizations$tree;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/lighting/DataLayerStorageMap;copy()Lnet/minecraft/world/level/lighting/DataLayerStorageMap;"))
    private DataLayerStorageMap<?> txoptimizations$initialSnapshot(DataLayerStorageMap<?> initialMap) {
        LightSnapshotMap source = (LightSnapshotMap) initialMap;

        if (!source.txoptimizations$sections().isEmpty())
            throw new IllegalStateException("Light storage was created with " + source.txoptimizations$sections().size() + " sections");

        BlockGetter level = this.chunkSource.getLevel();

        this.txoptimizations$tree = new LightSectionTree(level.getMinSectionY() - LevelLightEngine.LIGHT_SECTION_PADDING, level.getSectionsCount() + 2 * LevelLightEngine.LIGHT_SECTION_PADDING);

        return source.txoptimizations$snapshotWith(this.txoptimizations$tree);
    }

    @Redirect(method = "swapSectionMap", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/lighting/DataLayerStorageMap;copy()Lnet/minecraft/world/level/lighting/DataLayerStorageMap;"))
    private DataLayerStorageMap<?> txoptimizations$snapshot(DataLayerStorageMap<?> updating) {
        LightSnapshotMap source = (LightSnapshotMap) updating;

        if (this.chunkSource instanceof ClientChunkCache)
            BlockEntityLight.changed();

        this.txoptimizations$tree = this.txoptimizations$tree.with(this.changedSections, source);

        return source.txoptimizations$snapshotWith(this.txoptimizations$tree);
    }

    @Inject(method = "setLightEnabled", at = @At("HEAD"))
    private void txoptimizations$neighborsChanged(long zeroNode, boolean enable, CallbackInfo ci) {
        if (this.chunkSource instanceof ClientChunkCache)
            SectionNeighbors.changed();
    }
}
