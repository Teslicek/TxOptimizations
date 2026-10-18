package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.teslicek.txoptimizations.mixin.BufferBuilderAccessor;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryUtil;

public final class GlyphQuads {

    private static final VertexFormat LIT            = DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR;
    private static final VertexFormat UNLIT          = DefaultVertexFormat.POSITION_TEX_COLOR;
    private static final int          LIT_POSITION   = QuadVertices.offset(LIT, "Position");
    private static final int          LIT_UV         = QuadVertices.offset(LIT, "UV0");
    private static final int          LIT_COLOR      = QuadVertices.offset(LIT, "Color");
    private static final int          LIT_LIGHT      = QuadVertices.offset(LIT, "UV2");
    private static final int          UNLIT_POSITION = QuadVertices.offset(UNLIT, "Position");
    private static final int          UNLIT_UV       = QuadVertices.offset(UNLIT, "UV0");
    private static final int          UNLIT_COLOR    = QuadVertices.offset(UNLIT, "Color");

    private GlyphQuads() {
    }

    public static boolean tryWrite(VertexConsumer builder, Matrix4fc pose, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, float z, int color, float u0, float v0, float u1, float v1, int light) {
        if (!(builder instanceof QuadVertexBuffer quads) || pose.getClass() != Matrix4f.class)
            return false;

        VertexFormat format = quads.txoptimizations$quadFormat();

        if (format != LIT && format != UNLIT)
            return false;

        long pointer = quads.txoptimizations$tryReserveQuad(format);

        if (pointer == MemoryUtil.NULL)
            return false;

        boolean lit    = format == LIT;
        long    stride = format.getVertexSize();

        putVertex(pointer, lit, pose, x0, y0, z, color, u0, v0, light);
        putVertex(pointer + stride, lit, pose, x1, y1, z, color, u0, v1, light);
        putVertex(pointer + 2L * stride, lit, pose, x2, y2, z, color, u1, v1, light);
        putVertex(pointer + 3L * stride, lit, pose, x3, y3, z, color, u1, v0, light);

        return true;
    }

    private static void putVertex(long pointer, boolean lit, Matrix4fc pose, float x, float y, float z, int color, float u, float v, int light) {
        long uv = pointer + (lit ? LIT_UV : UNLIT_UV);

        QuadVertices.putPosition(pointer + (lit ? LIT_POSITION : UNLIT_POSITION), pose, x, y, z);
        BufferBuilderAccessor.txoptimizations$putRgba(pointer + (lit ? LIT_COLOR : UNLIT_COLOR), color);
        MemoryUtil.memPutFloat(uv, u);
        MemoryUtil.memPutFloat(uv + 4L, v);

        if (lit)
            BufferBuilderAccessor.txoptimizations$putPackedUv(pointer + LIT_LIGHT, light);
    }
}
