package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.ColorAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.PositionAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.TextureAttribute;

public final class TextVertex {

    public static final VertexFormat FORMAT       = DefaultVertexFormat.POSITION_TEX_COLOR;
    public static final int          STRIDE       = 24;
    private static final long        UV_OFFSET    = 12L;
    private static final long        COLOR_OFFSET = 20L;

    static {
        if (FORMAT.getVertexSize() != STRIDE)
            throw new IllegalStateException("POSITION_TEX_COLOR is " + FORMAT.getVertexSize() + " bytes per vertex, expected " + STRIDE);
    }

    private TextVertex() {
    }

    public static void put(long pointer, float x, float y, float z, int color, float u, float v) {
        PositionAttribute.put(pointer, x, y, z);
        TextureAttribute.put(pointer + UV_OFFSET, u, v);
        ColorAttribute.set(pointer + COLOR_OFFSET, color);
    }
}
