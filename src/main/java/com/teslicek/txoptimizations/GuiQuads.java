package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import org.joml.Matrix3x2fc;
import org.lwjgl.system.MemoryUtil;

public final class GuiQuads {

    private static final int          VERTICES       = 4;
    private static final VertexFormat COLORED        = DefaultVertexFormat.POSITION_COLOR;
    private static final int          COLORED_STRIDE = COLORED.getVertexSize();
    private static final int          COLORED_COLOR  = TexturedVertexLayout.offset(COLORED, DefaultVertexFormat.COLOR_SEMANTIC_NAME);

    private GuiQuads() {
    }

    public static boolean tryPutTextured(VertexConsumer builder, Matrix3x2fc pose, float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, int color) {
        if (!(builder instanceof DirectVertexBuffer direct) || direct.txoptimizations$format() != TexturedVertexLayout.UNLIT_GLYPH.format())
            return false;

        TexturedVertexLayout layout  = TexturedVertexLayout.UNLIT_GLYPH;
        long                 pointer = direct.txoptimizations$tryReserveVertices(layout.format(), VERTICES);

        if (pointer == MemoryUtil.NULL)
            return false;

        int stride = layout.stride();

        layout.put(pointer, x(pose, x0, y0), y(pose, x0, y0), 0.0F, color, u0, v0, 0);
        layout.put(pointer + stride, x(pose, x0, y1), y(pose, x0, y1), 0.0F, color, u0, v1, 0);
        layout.put(pointer + 2L * stride, x(pose, x1, y1), y(pose, x1, y1), 0.0F, color, u1, v1, 0);
        layout.put(pointer + 3L * stride, x(pose, x1, y0), y(pose, x1, y0), 0.0F, color, u1, v0, 0);

        return true;
    }

    public static boolean tryPutColored(VertexConsumer builder, Matrix3x2fc pose, float x0, float y0, float x1, float y1, int top, int bottom) {
        if (!(builder instanceof DirectVertexBuffer direct) || direct.txoptimizations$format() != COLORED)
            return false;

        long pointer = direct.txoptimizations$tryReserveVertices(COLORED, VERTICES);

        if (pointer == MemoryUtil.NULL)
            return false;

        putColored(pointer, pose, x0, y0, top);
        putColored(pointer + COLORED_STRIDE, pose, x0, y1, bottom);
        putColored(pointer + 2L * COLORED_STRIDE, pose, x1, y1, bottom);
        putColored(pointer + 3L * COLORED_STRIDE, pose, x1, y0, top);

        return true;
    }

    private static void putColored(long pointer, Matrix3x2fc pose, float x, float y, int color) {
        VertexBytes.putPosition(pointer, x(pose, x, y), y(pose, x, y), 0.0F);
        VertexBytes.putColor(pointer + COLORED_COLOR, color);
    }

    private static float x(Matrix3x2fc pose, float x, float y) {
        return pose.m00() * x + pose.m10() * y + pose.m20();
    }

    private static float y(Matrix3x2fc pose, float x, float y) {
        return pose.m01() * x + pose.m11() * y + pose.m21();
    }
}
