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
    private static final long   LINE_EDGE_BYTES    = 4L * LINE_STRIDE;

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

    public static void putLineSegment(long pointer, float startX, float startY, float startZ, float endX, float endY, float endZ, int colorBits, long tail) {
        requireLittleEndianLines();

        long color = (long) colorBits << Integer.SIZE;

        putLineSegmentWords(pointer, lineXy(startX, startY), color | lineZ(startZ), lineXy(endX, endY), color | lineZ(endZ), tail);
    }

    public static void putLineBox(long pointer, float x0, float y0, float z0, float x1, float y1, float z1, int colorBits, int xColorBits, int yColorBits, int zColorBits, float width) {
        requireLittleEndianLines();

        long x0y0  = lineXy(x0, y0);
        long x1y0  = lineXy(x1, y0);
        long x0y1  = lineXy(x0, y1);
        long x1y1  = lineXy(x1, y1);
        long front = lineZ(z0);
        long back  = lineZ(z1);
        long box   = (long) colorBits << Integer.SIZE;
        long xEdge = (long) xColorBits << Integer.SIZE;
        long yEdge = (long) yColorBits << Integer.SIZE;
        long zEdge = (long) zColorBits << Integer.SIZE;
        long posX  = lineTail(packNormal(x1 - x0, 0.0F, 0.0F), width);
        long negX  = lineTail(packNormal(x0 - x1, 0.0F, 0.0F), width);
        long posY  = lineTail(packNormal(0.0F, y1 - y0, 0.0F), width);
        long negY  = lineTail(packNormal(0.0F, y0 - y1, 0.0F), width);
        long posZ  = lineTail(packNormal(0.0F, 0.0F, z1 - z0), width);
        long negZ  = lineTail(packNormal(0.0F, 0.0F, z0 - z1), width);

        putLineEdgeWords(pointer, x0y0, xEdge | front, x1y0, xEdge | front, posX);
        putLineEdgeWords(pointer + LINE_EDGE_BYTES, x0y0, yEdge | front, x0y1, yEdge | front, posY);
        putLineEdgeWords(pointer + 2L * LINE_EDGE_BYTES, x0y0, zEdge | front, x0y0, zEdge | back, posZ);
        putLineEdgeWords(pointer + 3L * LINE_EDGE_BYTES, x1y0, box | front, x1y1, box | front, posY);
        putLineEdgeWords(pointer + 4L * LINE_EDGE_BYTES, x1y1, box | front, x0y1, box | front, negX);
        putLineEdgeWords(pointer + 5L * LINE_EDGE_BYTES, x0y1, box | front, x0y1, box | back, posZ);
        putLineEdgeWords(pointer + 6L * LINE_EDGE_BYTES, x0y1, box | back, x0y0, box | back, negY);
        putLineEdgeWords(pointer + 7L * LINE_EDGE_BYTES, x0y0, box | back, x1y0, box | back, posX);
        putLineEdgeWords(pointer + 8L * LINE_EDGE_BYTES, x1y0, box | back, x1y0, box | front, negZ);
        putLineEdgeWords(pointer + 9L * LINE_EDGE_BYTES, x0y1, box | back, x1y1, box | back, posX);
        putLineEdgeWords(pointer + 10L * LINE_EDGE_BYTES, x1y0, box | back, x1y1, box | back, posY);
        putLineSegmentWords(pointer + 11L * LINE_EDGE_BYTES, x1y1, box | front, x1y1, box | back, posZ);
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

    private static void requireLittleEndianLines() {
        if (!LITTLE_ENDIAN)
            throw new IllegalStateException("Line vertices are only packed into words on little-endian CPUs");
    }

    private static long lineXy(float x, float y) {
        return (long) Float.floatToRawIntBits(y) << Integer.SIZE | Float.floatToRawIntBits(x) & INT_MASK;
    }

    private static long lineZ(float z) {
        return Float.floatToRawIntBits(z) & INT_MASK;
    }

    private static void putLineEdgeWords(long pointer, long startXy, long startZ, long endXy, long endZ, long tail) {
        putLineSegmentWords(pointer, startXy, startZ, endXy, endZ, tail);
        putLineVertexWords(pointer + 3L * LINE_STRIDE, endXy, endZ, tail);
    }

    private static void putLineSegmentWords(long pointer, long startXy, long startZ, long endXy, long endZ, long tail) {
        putLineVertexWords(pointer, startXy, startZ, tail);
        putLineVertexWords(pointer + LINE_STRIDE, startXy, startZ, tail);
        putLineVertexWords(pointer + 2L * LINE_STRIDE, endXy, endZ, tail);
    }

    private static void putLineVertexWords(long pointer, long xy, long zColor, long tail) {
        MemoryUtil.memPutLong(pointer, xy);
        MemoryUtil.memPutLong(pointer + 8L, zColor);
        MemoryUtil.memPutLong(pointer + 16L, tail);
    }
}
