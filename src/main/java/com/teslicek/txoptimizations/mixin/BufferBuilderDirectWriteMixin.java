package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.teslicek.txoptimizations.DirectVertexBuffer;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderDirectWriteMixin implements DirectVertexBuffer {

    @Shadow
    @Final
    private static int MAX_VERTEX_COUNT;

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
    private boolean building;

    @Shadow
    private void endLastVertex() {
        throw new AssertionError();
    }

    @Override
    public VertexFormat txoptimizations$format() {
        return this.format;
    }

    @Override
    public long txoptimizations$tryReserveVertices(VertexFormat format, int count) {
        if (count <= 0)
            throw new IllegalArgumentException("Cannot reserve " + count + " vertices");

        if (format != this.format || !this.building || this.primitiveTopology == PrimitiveTopology.LINES || this.vertices > MAX_VERTEX_COUNT - count || ((Object) this).getClass() != BufferBuilder.class)
            return MemoryUtil.NULL;

        ByteBufferBuilderAccessor buffer = (ByteBufferBuilderAccessor) this.buffer;

        if (buffer.txoptimizations$writeOffset() + (long) count * this.vertexSize > buffer.txoptimizations$capacity())
            return MemoryUtil.NULL;

        this.endLastVertex();

        return this.txoptimizations$reserve(count);
    }

    @Override
    public boolean txoptimizations$duplicatesVertices() {
        return this.primitiveTopology == PrimitiveTopology.LINES;
    }

    @Override
    public long txoptimizations$reserveAfterLastVertex(int count) {
        ByteBufferBuilderAccessor buffer = (ByteBufferBuilderAccessor) this.buffer;
        int                       length = (count + 1) * this.vertexSize;

        if (this.vertices == 0 || this.elementsToFill != 0 || this.primitiveTopology != PrimitiveTopology.LINES || buffer.txoptimizations$writeOffset() + length > buffer.txoptimizations$capacity()) {
            this.endLastVertex();

            return this.txoptimizations$reserve(count);
        }

        long duplicate = this.buffer.reserve(length);

        MemoryUtil.memCopy(duplicate - this.vertexSize, duplicate, this.vertexSize);

        this.vertices       += count + 1;
        this.vertexPointer   = duplicate + length - this.vertexSize;
        this.elementsToFill  = 0;

        return duplicate + this.vertexSize;
    }

    @Unique
    private long txoptimizations$reserve(int count) {
        int  length      = count * this.vertexSize;
        long destination = this.buffer.reserve(length);

        this.vertices      += count;
        this.vertexPointer  = destination + length - this.vertexSize;
        this.elementsToFill = 0;

        return destination;
    }
}
