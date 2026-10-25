package com.teslicek.txoptimizations;

import net.minecraft.world.level.chunk.ChunkAccess;

public interface DeferredSkyLightSources {

    void txoptimizations$fillLater(ChunkAccess chunk);
}
