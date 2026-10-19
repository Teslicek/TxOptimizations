package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.vertex.VertexFormat;
import java.nio.ByteOrder;
import net.minecraft.util.ARGB;
import org.lwjgl.system.MemoryUtil;

public final class VertexBytes {

    public static final int     ENTITY_STRIDE = 36;
    public static final int     LINE_STRIDE   = 24;
    public static final boolean LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    private static final String POSITION           = "Position";
    private static final String COLOR              = "Color";
    private static final String NORMAL             = "Normal";
    private static final String LINE_WIDTH         = "LineWidth";
    private static final int    LINE_COLOR_OFFSET  = 12;
    private static final int    LINE_NORMAL_OFFSET = 16;
    private static final int    LINE_WIDTH_OFFSET  = 20;
    private static final int    BYTE_MASK          = 0xFF;
    private static final int    HALF_BITS          = 16;
    private static final long   INT_MASK           = 0xFFFFFFFFL;
    private static final byte   NORMAL_SCALE       = 127;

    private VertexBytes() {
    }

    public static void putPosition(long pointer, float x, float y, float z) {
        MemoryUtil.memPutFloat(pointer, x);
        MemoryUtil.memPutFloat(pointer + 4L, y);
        MemoryUtil.memPutFloat(pointer + 8L, z);
    }

    public static void putUv(long pointer, float u, float v) {
        MemoryUtil.memPutFloat(pointer, u);
        MemoryUtil.memPutFloat(pointer + 4L, v);
    }

    public static void putColor(long pointer, int argb) {
        MemoryUtil.memPutInt(pointer, colorBits(argb));
    }

    public static int colorBits(int argb) {
        int abgr = ARGB.toABGR(argb);

        return LITTLE_ENDIAN ? abgr : Integer.reverseBytes(abgr);
    }

    public static void putPacked(long pointer, int packed) {
        MemoryUtil.memPutInt(pointer, packedBits(packed));
    }

    public static long pairBits(int first, int second) {
        return LITTLE_ENDIAN ? (long) second << Integer.SIZE | first & INT_MASK : (long) packedBits(first) << Integer.SIZE | packedBits(second) & INT_MASK;
    }

    public static void putNormal(long pointer, int packed) {
        MemoryUtil.memPutShort(pointer, (short) (LITTLE_ENDIAN ? packed : (packed & BYTE_MASK) << Byte.SIZE | packed >> Byte.SIZE & BYTE_MASK));
        MemoryUtil.memPutByte(pointer + 2L, (byte) (packed >> HALF_BITS));
    }

    public static void putEntity(long pointer, float x, float y, float z, int argb, float u, float v, int overlay, int light, int normal) {
        putEntityBits(pointer, x, y, z, colorBits(argb), u, v, pairBits(overlay, light), normal);
    }

    public static void putEntityBits(long pointer, float x, float y, float z, int colorBits, float u, float v, long coordinateBits, int normal) {
        putPosition(pointer, x, y, z);
        MemoryUtil.memPutInt(pointer + 12L, colorBits);
        putUv(pointer + 16L, u, v);
        MemoryUtil.memPutLong(pointer + 24L, coordinateBits);
        putNormal(pointer + 32L, normal);
    }

    public static void putBlock(long pointer, float x, float y, float z, int argb, float u, float v, int light) {
        putPosition(pointer, x, y, z);
        putColor(pointer + 12L, argb);
        putUv(pointer + 16L, u, v);
        putPacked(pointer + 24L, light);
    }

    public static byte normalByte(float value) {
        if (value < -1.0F)
            return -NORMAL_SCALE;

        if (value > 1.0F)
            return NORMAL_SCALE;

        return (byte) (int) (value * NORMAL_SCALE);
    }

    public static void putLine(long pointer, float x, float y, float z, int colorBits, long tail) {
        if (!LITTLE_ENDIAN)
            throw new IllegalStateException("Line vertices are only packed into words on little-endian CPUs");

        MemoryUtil.memPutLong(pointer, (long) Float.floatToRawIntBits(y) << Integer.SIZE | Float.floatToRawIntBits(x) & INT_MASK);
        MemoryUtil.memPutLong(pointer + 8L, (long) colorBits << Integer.SIZE | Float.floatToRawIntBits(z) & INT_MASK);
        MemoryUtil.memPutLong(pointer + 16L, tail);
    }

    public static void requireLineLayout(VertexFormat format) {
        if (format.getVertexSize() != LINE_STRIDE || format.getElement(POSITION).offset() != 0 || format.getElement(COLOR).offset() != LINE_COLOR_OFFSET || format.getElement(NORMAL).offset() != LINE_NORMAL_OFFSET || format.getElement(LINE_WIDTH).offset() != LINE_WIDTH_OFFSET)
            throw new IllegalStateException("Line vertex format " + format + " no longer has the position, color, normal and line width layout line vertices are packed in");
    }

    public static long lineTail(int packedNormal, float width) {
        return (long) Float.floatToRawIntBits(width) << Integer.SIZE | packedNormal & INT_MASK;
    }

    public static int packNormal(float x, float y, float z) {
        return normalByte(x) & BYTE_MASK | (normalByte(y) & BYTE_MASK) << Byte.SIZE | (normalByte(z) & BYTE_MASK) << HALF_BITS;
    }

    public static int flipNormal(int packed) {
        return -(byte) packed & BYTE_MASK | (-(byte) (packed >> Byte.SIZE) & BYTE_MASK) << Byte.SIZE | (-(byte) (packed >> HALF_BITS) & BYTE_MASK) << HALF_BITS;
    }

    private static int packedBits(int packed) {
        return LITTLE_ENDIAN ? packed : Integer.rotateLeft(packed, HALF_BITS);
    }
}
