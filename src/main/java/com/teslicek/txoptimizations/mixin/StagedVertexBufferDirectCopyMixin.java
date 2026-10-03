package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StagedVertexBuffer.class)
public abstract class StagedVertexBufferDirectCopyMixin {

    @Unique
    private static final ByteBuffer EMPTY = ByteBuffer.allocate(0);

    @Shadow
    @Final
    private ByteBufferBuilder stagingBuffer;

    @WrapOperation(method = "uploadDrawsToBuffers", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/ByteBufferBuilder$Result;byteBuffer()Ljava/nio/ByteBuffer;"))
    private ByteBuffer txoptimizations$copyDirectly(ByteBufferBuilder.Result slice, Operation<ByteBuffer> original, @Local ByteBuffer buffer) {
        ByteBufferBuilderAccessor builder = (ByteBufferBuilderAccessor) this.stagingBuffer;
        ByteBufferResultAccessor  result  = (ByteBufferResultAccessor) slice;
        int                       size    = slice.size();
        int                       start   = buffer.position();

        if (!builder.txoptimizations$isValid(result.txoptimizations$generation()))
            throw new IllegalStateException("Buffer is no longer valid");

        if (size > buffer.remaining())
            throw new BufferOverflowException();

        MemoryUtil.memCopy(builder.txoptimizations$pointer() + result.txoptimizations$offset(), MemoryUtil.memAddress(buffer), size);
        buffer.position(start + size);

        return EMPTY;
    }
}
