package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.teslicek.txoptimizations.DirectVertexBuffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderDirectWriteMixin implements DirectVertexBuffer {

    @Shadow
    private int vertices;

    @Shadow
    @Final
    private int vertexSize;

    @Shadow
    private long vertexPointer;

    @Shadow
    @Final
    private ByteBufferBuilder buffer;

    @Shadow
    private int elementsToFill;

    @Shadow
    @Final
    private VertexFormat format;

    @Shadow
    @Final
    private PrimitiveTopology primitiveTopology;

    @Shadow
    private void endLastVertex() {
        throw new AssertionError();
    }

    @Override
    public boolean txoptimizations$writesFormat(VertexFormat format) {
        return format == this.format;
    }

    @Override
    public long txoptimizations$reserveVertices(int count) {
        int  length      = count * this.vertexSize;
        long destination = this.buffer.reserve(length);

        this.vertices      += count;
        this.vertexPointer  = destination + length - this.vertexSize;
        this.elementsToFill = 0;

        return destination;
    }

    @Override
    public VertexFormat txoptimizations$format() {
        return this.format;
    }

    @Override
    public boolean txoptimizations$duplicatesVertices() {
        return this.primitiveTopology == PrimitiveTopology.LINES;
    }

    @Override
    public long txoptimizations$reserveAfterLastVertex(int count) {
        this.endLastVertex();

        return this.txoptimizations$reserveVertices(count);
    }
}
