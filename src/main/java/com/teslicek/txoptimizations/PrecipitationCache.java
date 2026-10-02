package com.teslicek.txoptimizations;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

public final class PrecipitationCache {

    private static final int                                         LIMIT = 16384;
    private static final Long2ObjectOpenHashMap<Biome.Precipitation> CACHE = new Long2ObjectOpenHashMap<>();

    private static ClientLevel level;
    private static long        version;
    private static long        cachedVersion;

    private PrecipitationCache() {
    }

    public static void invalidate() {
        version ++;
    }

    public static Biome.Precipitation get(ClientLevel currentLevel, BlockPos pos, Operation<Biome.Precipitation> lookup) {
        if (currentLevel != level || cachedVersion != version || CACHE.size() >= LIMIT) {
            CACHE.clear();
            level         = currentLevel;
            cachedVersion = version;
        }

        long                key           = pos.asLong();
        Biome.Precipitation precipitation = CACHE.get(key);

        if (precipitation != null)
            return precipitation;

        precipitation = lookup.call(currentLevel, pos);
        CACHE.put(key, precipitation);

        return precipitation;
    }
}
