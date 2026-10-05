package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.LayeredSectionMap;
import com.teslicek.txoptimizations.LightSnapshotMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LayerLightSectionStorage.class)
public abstract class LayerLightSectionStorageSnapshotMixin {

    @Unique
    private static final int MIN_CHANGES = 512;

    @Unique
    private static final int CHANGES_SHIFT = 4;

    @Shadow
    @Final
    protected LongSet changedSections;

    @Unique
    private Long2ObjectOpenHashMap<DataLayer> txoptimizations$base;

    @Unique
    private Long2ObjectOpenHashMap<DataLayer> txoptimizations$changes;

    @Redirect(method = "swapSectionMap", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/lighting/DataLayerStorageMap;copy()Lnet/minecraft/world/level/lighting/DataLayerStorageMap;"))
    private DataLayerStorageMap<?> txoptimizations$snapshot(DataLayerStorageMap<?> updating) {
        LightSnapshotMap                  source   = (LightSnapshotMap) updating;
        Long2ObjectOpenHashMap<DataLayer> sections = source.txoptimizations$sections();
        Long2ObjectOpenHashMap<DataLayer> previous = this.txoptimizations$changes;

        if (this.txoptimizations$base == null || previous.size() + this.changedSections.size() > Math.max(MIN_CHANGES, sections.size() >> CHANGES_SHIFT)) {
            DataLayerStorageMap<?> copy = updating.copy();

            this.txoptimizations$base    = ((LightSnapshotMap) copy).txoptimizations$sections();
            this.txoptimizations$changes = new Long2ObjectOpenHashMap<>();

            return copy;
        }

        Long2ObjectOpenHashMap<DataLayer> changes  = previous.clone();
        LongIterator                      iterator = this.changedSections.iterator();

        while (iterator.hasNext()) {
            long section = iterator.nextLong();

            changes.put(section, sections.get(section));
        }

        this.txoptimizations$changes = changes;

        return source.txoptimizations$snapshotWith(new LayeredSectionMap(this.txoptimizations$base, changes));
    }
}
