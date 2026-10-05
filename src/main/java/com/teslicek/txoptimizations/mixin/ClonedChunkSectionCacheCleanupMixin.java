package com.teslicek.txoptimizations.mixin;

import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import net.caffeinemc.mods.sodium.client.world.cloned.ClonedChunkSection;
import net.caffeinemc.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ClonedChunkSectionCache.class, remap = false)
public abstract class ClonedChunkSectionCacheCleanupMixin {

    @Shadow
    @Final
    private static long MAX_CACHE_DURATION;

    @Shadow
    @Final
    private Long2ReferenceLinkedOpenHashMap<ClonedChunkSection> positionToEntry;

    @Shadow
    private long time;

    @Shadow
    private static long getMonotonicTimeSource() {
        throw new AssertionError();
    }

    @Overwrite
    public void cleanup() {
        this.time = getMonotonicTimeSource();

        while (!this.positionToEntry.isEmpty() && this.time > this.positionToEntry.get(this.positionToEntry.firstLongKey()).getLastUsedTimestamp() + MAX_CACHE_DURATION)
            this.positionToEntry.removeFirst();
    }
}
