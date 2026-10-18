package com.teslicek.txoptimizations;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.jspecify.annotations.Nullable;

public interface SectionFacing {

    int @Nullable [] txoptimizations$facingRanges(ChunkSectionLayer layer);
}
