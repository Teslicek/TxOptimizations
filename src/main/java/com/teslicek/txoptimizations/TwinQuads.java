package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.CompactVectorArray;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import java.nio.ByteOrder;
import java.util.Arrays;
import org.lwjgl.system.MemoryUtil;

public final class TwinQuads extends CompactVectorArray {

    private static final int     VERTICES_PER_QUAD = PrimitiveTopology.QUADS.primitiveLength;
    private static final int     INDICES_PER_QUAD  = PrimitiveTopology.QUADS.indexCount(VERTICES_PER_QUAD);
    private static final int     WORD_BITS         = Long.SIZE;
    private static final int     WORD_SHIFT        = 6;
    private static final int[]   REVERSED          = {0, 3, 2, 1};
    private static final boolean LITTLE_ENDIAN     = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    private final long[] twins;
    private final int    indexCount;

    public TwinQuads(int quads, long[] twins) {
        super(quads);

        int count = 0;
        int last  = -1;

        for (int word = 0; word < twins.length; word ++) {
            count += Long.bitCount(twins[word]);

            if (twins[word] != 0L)
                last = word * WORD_BITS + WORD_BITS - 1 - Long.numberOfLeadingZeros(twins[word]);
        }

        if (last >= quads)
            throw new IllegalArgumentException("Quad " + last + " is marked as having a twin but only " + quads + " quads were kept");

        this.twins      = Arrays.copyOf(twins, words(quads));
        this.indexCount = (quads + count) * INDICES_PER_QUAD;
    }

    public static long[] twinWords(int quads) {
        return new long[words(quads)];
    }

    public static int share(long vertices, int vertexSize, int quads, long[] twins) {
        int     quadSize  = vertexSize * VERTICES_PER_QUAD;
        int     kept      = 0;
        boolean twinnable = false;

        for (int quad = 0; quad < quads; quad ++) {
            long source = vertices + (long) quad * quadSize;

            if (twinnable && reversedCopy(vertices + (long) (kept - 1) * quadSize, source, vertexSize)) {
                twins[kept - 1 >>> WORD_SHIFT] |= 1L << kept - 1;
                twinnable = false;
                continue;
            }

            if (kept != quad)
                MemoryUtil.memCopy(source, vertices + (long) kept * quadSize, quadSize);

            kept ++;
            twinnable = true;
        }

        return kept;
    }

    public static int indexCount(CompactVectorArray centroids) {
        return centroids instanceof TwinQuads twinQuads ? twinQuads.indexCount : centroids.size() * INDICES_PER_QUAD;
    }

    public static long writeIndices(long pointer, int[] quadOrder, CompactVectorArray centroids, boolean shortIndices) {
        if (!(centroids instanceof TwinQuads twinQuads))
            return writeIndices(pointer, quadOrder, shortIndices);

        long[] twins = twinQuads.twins;

        for (int quad : quadOrder) {
            int first = quad * VERTICES_PER_QUAD;

            pointer = writeQuad(pointer, first, first + 1, first + 2, first + 3, shortIndices);

            if ((twins[quad >>> WORD_SHIFT] & 1L << quad) != 0L)
                pointer = writeQuad(pointer, first, first + 3, first + 2, first + 1, shortIndices);
        }

        return pointer;
    }

    public static long writeIndices(long pointer, int[] quadOrder, boolean shortIndices) {
        for (int quad : quadOrder) {
            int first = quad * VERTICES_PER_QUAD;

            pointer = writeQuad(pointer, first, first + 1, first + 2, first + 3, shortIndices);
        }

        return pointer;
    }

    public static boolean shortIndices(IndexType type) {
        if (type != IndexType.SHORT && type != IndexType.INT)
            throw new IllegalStateException("Unknown index type " + type);

        return type == IndexType.SHORT;
    }

    private static long writeQuad(long pointer, int first, int second, int third, int fourth, boolean shortIndices) {
        if (LITTLE_ENDIAN)
            return writeQuadLittleEndian(pointer, first, second, third, fourth, shortIndices);

        if (shortIndices) {
            MemoryUtil.memPutShort(pointer, (short) first);
            MemoryUtil.memPutShort(pointer + 2L, (short) second);
            MemoryUtil.memPutShort(pointer + 4L, (short) third);
            MemoryUtil.memPutShort(pointer + 6L, (short) third);
            MemoryUtil.memPutShort(pointer + 8L, (short) fourth);
            MemoryUtil.memPutShort(pointer + 10L, (short) first);

            return pointer + INDICES_PER_QUAD * Short.BYTES;
        }

        MemoryUtil.memPutInt(pointer, first);
        MemoryUtil.memPutInt(pointer + 4L, second);
        MemoryUtil.memPutInt(pointer + 8L, third);
        MemoryUtil.memPutInt(pointer + 12L, third);
        MemoryUtil.memPutInt(pointer + 16L, fourth);
        MemoryUtil.memPutInt(pointer + 20L, first);

        return pointer + INDICES_PER_QUAD * Integer.BYTES;
    }

    private static long writeQuadLittleEndian(long pointer, int first, int second, int third, int fourth, boolean shortIndices) {
        if (shortIndices) {
            long low  = first & 0xFFFFL;
            long high = (long) (second & 0xFFFF) << 16 | (long) (third & 0xFFFF) << 32 | (long) (third & 0xFFFF) << 48;

            MemoryUtil.memPutLong(pointer, low | high);
            MemoryUtil.memPutInt(pointer + 8L, fourth & 0xFFFF | (first & 0xFFFF) << 16);

            return pointer + INDICES_PER_QUAD * Short.BYTES;
        }

        MemoryUtil.memPutLong(pointer, first & 0xFFFFFFFFL | (long) second << 32);
        MemoryUtil.memPutLong(pointer + 8L, third & 0xFFFFFFFFL | (long) third << 32);
        MemoryUtil.memPutLong(pointer + 16L, fourth & 0xFFFFFFFFL | (long) first << 32);

        return pointer + INDICES_PER_QUAD * Integer.BYTES;
    }

    private static boolean reversedCopy(long quad, long candidate, int vertexSize) {
        for (int vertex = 0; vertex < VERTICES_PER_QUAD; vertex ++) {
            if (!sameBytes(quad + (long) REVERSED[vertex] * vertexSize, candidate + (long) vertex * vertexSize, vertexSize))
                return false;
        }

        return true;
    }

    private static boolean sameBytes(long first, long second, int size) {
        int offset = 0;

        for (; offset + Long.BYTES <= size; offset += Long.BYTES) {
            if (MemoryUtil.memGetLong(first + offset) != MemoryUtil.memGetLong(second + offset))
                return false;
        }

        for (; offset < size; offset ++) {
            if (MemoryUtil.memGetByte(first + offset) != MemoryUtil.memGetByte(second + offset))
                return false;
        }

        return true;
    }

    private static int words(int quads) {
        return (quads + WORD_BITS - 1) >>> WORD_SHIFT;
    }
}
