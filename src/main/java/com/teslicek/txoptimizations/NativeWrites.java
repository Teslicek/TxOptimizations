package com.teslicek.txoptimizations;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.lwjgl.system.MemoryUtil;

public final class NativeWrites {

    private NativeWrites() {
    }

    public static long reserve(ByteBuffer buffer, int bytes) {
        requireNative(buffer);

        int position = buffer.position();

        if (buffer.limit() - position < bytes)
            throw new BufferOverflowException();

        buffer.position(position + bytes);

        return MemoryUtil.memAddress0(buffer) + position;
    }

    public static long at(ByteBuffer buffer, int offset, int bytes) {
        requireNative(buffer);

        if (offset < 0 || bytes < 0 || buffer.capacity() - offset < bytes)
            throw new IndexOutOfBoundsException(bytes + " bytes at " + offset + " do not fit in " + buffer);

        return MemoryUtil.memAddress0(buffer) + offset;
    }

    private static void requireNative(ByteBuffer buffer) {
        if (!buffer.isDirect() || buffer.isReadOnly() || buffer.order() != ByteOrder.nativeOrder())
            throw new IllegalArgumentException("Expected a writable native order direct buffer but got " + buffer + " in " + buffer.order());
    }
}
