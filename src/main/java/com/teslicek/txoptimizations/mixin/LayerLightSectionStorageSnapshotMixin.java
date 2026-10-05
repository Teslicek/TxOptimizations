package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.LayeredSectionMap;
import com.teslicek.txoptimizations.LightSnapshotMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
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
    private static final int CHANGES_SHIFT = 3;

    @Shadow
    @Final
    protected LongSet changedSections;

    @Unique
    private Long2ObjectOpenHashMap<DataLayer> txoptimizations$base;

    @Unique
    private LongOpenHashSet txoptimizations$changedSinceBase;

    @Redirect(method = "swapSectionMap", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/lighting/DataLayerStorageMap;copy()Lnet/minecraft/world/level/lighting/DataLayerStorageMap;"))
    private DataLayerStorageMap<?> txoptimizations$snapshot(DataLayerStorageMap<?> updating) {
        LightSnapshotMap                  source   = (LightSnapshotMap) updating;
        Long2ObjectOpenHashMap<DataLayer> sections = source.txoptimizations$sections();

        if (this.txoptimizations$changedSinceBase == null)
            this.txoptimizations$changedSinceBase = new LongOpenHashSet();

        LongOpenHashSet changed = this.txoptimizations$changedSinceBase;

        changed.addAll(this.changedSections);

        if (this.txoptimizations$base == null || changed.size() > Math.max(MIN_CHANGES, sections.size() >> CHANGES_SHIFT)) {
            DataLayerStorageMap<?> copy = updating.copy();

            this.txoptimizations$base = ((LightSnapshotMap) copy).txoptimizations$sections();
            changed.clear();

            return copy;
        }

        Long2ObjectOpenHashMap<DataLayer> changes  = new Long2ObjectOpenHashMap<>(changed.size());
        LongIterator                      iterator = changed.iterator();

        while (iterator.hasNext()) {
            long section = iterator.nextLong();

            changes.put(section, sections.get(section));
        }

        return source.txoptimizations$snapshotWith(new LayeredSectionMap(this.txoptimizations$base, changes));
    }
}
