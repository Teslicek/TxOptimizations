package com.teslicek.txoptimizations;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.lwjgl.system.MemoryUtil;

public final class NativeWrites {

    private NativeWrites() {
    }

    public static long reserve(ByteBuffer buffer, int bytes) {
        if (!buffer.isDirect() || buffer.isReadOnly() || buffer.order() != ByteOrder.nativeOrder())
            throw new IllegalArgumentException("Expected a writable native order direct buffer but got " + buffer + " in " + buffer.order());

        int position = buffer.position();

        if (buffer.limit() - position < bytes)
            throw new BufferOverflowException();

        buffer.position(position + bytes);

        return MemoryUtil.memAddress0(buffer) + position;
    }
}
