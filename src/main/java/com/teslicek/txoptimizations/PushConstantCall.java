package com.teslicek.txoptimizations;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;

public final class PushConstantCall {

    private static final MethodHandle CALL = Linker.nativeLinker().downcallHandle(FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));

    private static long          functionAddress;
    private static MemorySegment function;

    private PushConstantCall() {
    }

    public static void push(VkCommandBuffer commandBuffer, long layout, int stageFlags, int offset, ByteBuffer values) {
        long address = commandBuffer.getCapabilities().vkCmdPushConstants;

        if (address == 0L)
            throw new IllegalStateException("vkCmdPushConstants is not loaded on this device");

        if (address != functionAddress) {
            function        = MemorySegment.ofAddress(address);
            functionAddress = address;
        }

        try {
            CALL.invokeExact(function, commandBuffer.address(), layout, stageFlags, offset, values.remaining(), MemoryUtil.memAddress(values));
        } catch (Throwable throwable) {
            throw new IllegalStateException("vkCmdPushConstants failed", throwable);
        }
    }
}
