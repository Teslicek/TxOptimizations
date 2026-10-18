package com.teslicek.txoptimizations;

import java.nio.ByteOrder;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.lwjgl.system.MemoryUtil;

public final class VertexBytes {

    public static final int ENTITY_STRIDE = 36;

    private static final boolean LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    private static final int     BYTE_MASK     = 0xFF;
    private static final int     SHORT_MASK    = 0xFFFF;

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
        int abgr = ARGB.toABGR(argb);

        MemoryUtil.memPutInt(pointer, LITTLE_ENDIAN ? abgr : Integer.reverseBytes(abgr));
    }

    public static void putPacked(long pointer, int packed) {
        if (LITTLE_ENDIAN) {
            MemoryUtil.memPutInt(pointer, packed);

            return;
        }

        MemoryUtil.memPutShort(pointer, (short) (packed & SHORT_MASK));
        MemoryUtil.memPutShort(pointer + 2L, (short) (packed >> 16 & SHORT_MASK));
    }

    public static void putNormal(long pointer, int packed) {
        MemoryUtil.memPutByte(pointer, (byte) packed);
        MemoryUtil.memPutByte(pointer + 1L, (byte) (packed >> 8));
        MemoryUtil.memPutByte(pointer + 2L, (byte) (packed >> 16));
    }

    public static void putEntity(long pointer, float x, float y, float z, int argb, float u, float v, int overlay, int light, int normal) {
        putPosition(pointer, x, y, z);
        putColor(pointer + 12L, argb);
        putUv(pointer + 16L, u, v);
        putPacked(pointer + 24L, overlay);
        putPacked(pointer + 28L, light);
        putNormal(pointer + 32L, normal);
    }

    public static byte normalByte(float value) {
        return (byte) ((int) (Mth.clamp(value, -1.0F, 1.0F) * 127.0F) & BYTE_MASK);
    }

    public static int packNormal(float x, float y, float z) {
        return normalByte(x) & BYTE_MASK | (normalByte(y) & BYTE_MASK) << 8 | (normalByte(z) & BYTE_MASK) << 16;
    }

    public static int flipNormal(int packed) {
        return -(byte) packed & BYTE_MASK | (-(byte) (packed >> 8) & BYTE_MASK) << 8 | (-(byte) (packed >> 16) & BYTE_MASK) << 16;
    }
}
