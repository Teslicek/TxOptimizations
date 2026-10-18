package com.teslicek.txoptimizations;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public final class SectionLayers {

    public static final ChunkSectionLayer[] VALUES      = ChunkSectionLayer.values();
    public static final boolean[]           TRANSLUCENT = translucent();

    private SectionLayers() {
    }

    private static boolean[] translucent() {
        boolean[] translucent = new boolean[VALUES.length];

        for (ChunkSectionLayer layer : VALUES)
            translucent[layer.ordinal()] = layer.translucent();

        return translucent;
    }
}
