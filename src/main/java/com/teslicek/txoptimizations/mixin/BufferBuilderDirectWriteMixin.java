package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.teslicek.txoptimizations.DirectVertexWriter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderDirectWriteMixin implements DirectVertexWriter {

    @Shadow
    @Final
    private ByteBufferBuilder buffer;

    @Shadow
    @Final
    private VertexFormat format;

    @Shadow
    @Final
    private int vertexSize;

    @Shadow
    private int vertices;

    @Shadow
    private long vertexPointer;

    @Shadow
    private int elementsToFill;

    @Override
    public long txoptimizations$reserveVertices(int count, VertexFormat format) {
        if (format != this.format)
            throw new IllegalStateException("Direct vertex write in " + format + " into a buffer of " + this.format);

        int  length      = count * this.vertexSize;
        long destination = this.buffer.reserve(length);

        this.vertices      += count;
        this.vertexPointer  = destination + length - this.vertexSize;
        this.elementsToFill = 0;

        return destination;
    }
}
