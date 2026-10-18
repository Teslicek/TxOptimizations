package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import java.util.Set;

public record TexturedVertexLayout(VertexFormat format, int stride, int position, int color, int uv, int light) {

    private static final int         NO_ELEMENT = -1;
    private static final Set<String> ELEMENTS   = Set.of(DefaultVertexFormat.POSITION_SEMANTIC_NAME, DefaultVertexFormat.COLOR_SEMANTIC_NAME, DefaultVertexFormat.UV0_SEMANTIC_NAME, DefaultVertexFormat.UV2_SEMANTIC_NAME);

    public static final TexturedVertexLayout LIT_GLYPH   = of(DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR);
    public static final TexturedVertexLayout UNLIT_GLYPH = of(DefaultVertexFormat.POSITION_TEX_COLOR);
    public static final TexturedVertexLayout PARTICLE    = of(DefaultVertexFormat.PARTICLE);

    public static TexturedVertexLayout glyph(VertexFormat format) {
        if (format == LIT_GLYPH.format)
            return LIT_GLYPH;

        if (format == UNLIT_GLYPH.format)
            return UNLIT_GLYPH;

        return null;
    }

    public void put(long pointer, float x, float y, float z, int argb, float u, float v, int packedLight) {
        VertexBytes.putPosition(pointer + this.position, x, y, z);
        VertexBytes.putColor(pointer + this.color, argb);
        VertexBytes.putUv(pointer + this.uv, u, v);

        if (this.light != NO_ELEMENT)
            VertexBytes.putPacked(pointer + this.light, packedLight);
    }

    private static TexturedVertexLayout of(VertexFormat format) {
        for (VertexFormatElement element : format.getElements()) {
            if (!ELEMENTS.contains(element.name()))
                throw new IllegalStateException("Vertex format " + format + " has an element a textured quad does not write: " + element);
        }

        return new TexturedVertexLayout(format, format.getVertexSize(), offset(format, DefaultVertexFormat.POSITION_SEMANTIC_NAME), offset(format, DefaultVertexFormat.COLOR_SEMANTIC_NAME), offset(format, DefaultVertexFormat.UV0_SEMANTIC_NAME), optionalOffset(format, DefaultVertexFormat.UV2_SEMANTIC_NAME));
    }

    private static int offset(VertexFormat format, String name) {
        VertexFormatElement element = format.getElement(name);

        if (element == null)
            throw new IllegalStateException("Vertex format " + format + " has no " + name + " element");

        return element.offset();
    }

    private static int optionalOffset(VertexFormat format, String name) {
        VertexFormatElement element = format.getElement(name);

        return element == null ? NO_ELEMENT : element.offset();
    }
}
