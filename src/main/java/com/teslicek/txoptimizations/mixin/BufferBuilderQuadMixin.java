package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.teslicek.txoptimizations.QuadVertexBuffer;
import com.teslicek.txoptimizations.QuadVertices;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderQuadMixin implements VertexConsumer, QuadVertexBuffer {

    @Shadow
    @Final
    private static int MAX_VERTEX_COUNT;

    @Shadow
    @Final
    private ByteBufferBuilder buffer;

    @Shadow
    private long vertexPointer;

    @Shadow
    private int vertices;

    @Shadow
    @Final
    private VertexFormat format;

    @Shadow
    @Final
    private PrimitiveTopology primitiveTopology;

    @Shadow
    @Final
    private boolean blockFormat;

    @Shadow
    @Final
    private boolean entityFormat;

    @Shadow
    @Final
    private int vertexSize;

    @Shadow
    private boolean building;

    @Shadow
    private void endLastVertex() {
        throw new AssertionError();
    }

    @Shadow
    private static void putRgba(long pointer, int argb) {
        throw new AssertionError();
    }

    @Shadow
    private static void putPackedUv(long pointer, int packedUv) {
        throw new AssertionError();
    }

    @Shadow
    private static byte normalIntValue(float value) {
        throw new AssertionError();
    }

    @Override
    public VertexFormat txoptimizations$quadFormat() {
        return this.format;
    }

    @Override
    public long txoptimizations$tryReserveQuad(VertexFormat format) {
        if (format != this.format || !this.building || this.primitiveTopology == PrimitiveTopology.LINES || this.vertices > MAX_VERTEX_COUNT - QuadVertices.COUNT || ((Object) this).getClass() != BufferBuilder.class)
            return MemoryUtil.NULL;

        ByteBufferBuilderAccessor buffer = (ByteBufferBuilderAccessor) this.buffer;
        int                       length = QuadVertices.COUNT * this.vertexSize;

        if (buffer.txoptimizations$writeOffset() + length > buffer.txoptimizations$capacity())
            return MemoryUtil.NULL;

        this.endLastVertex();

        long pointer = this.buffer.reserve(length);

        this.vertices      += QuadVertices.COUNT;
        this.vertexPointer  = pointer + length - this.vertexSize;

        return pointer;
    }

    @Override
    public void putBakedQuad(PoseStack.Pose pose, BakedQuad quad, QuadInstance instance) {
        Matrix4f matrix        = pose.pose();
        Vector3f normal        = pose.transformNormal(quad.direction().getUnitVec3f(), new Vector3f());
        int      lightEmission = quad.materialInfo().lightEmission();
        long     pointer       = this.blockFormat || this.entityFormat ? this.txoptimizations$tryReserveQuad(this.format) : MemoryUtil.NULL;

        if (pointer == MemoryUtil.NULL) {
            for (int vertex = 0; vertex < QuadVertices.COUNT; vertex ++) {
                Vector3f position = matrix.transformPosition(quad.position(vertex), new Vector3f());
                long     packedUv = quad.packedUV(vertex);

                this.addVertex(position.x(), position.y(), position.z(), instance.getColor(vertex), UVPair.unpackU(packedUv), UVPair.unpackV(packedUv), instance.overlayCoords(), instance.getLightCoordsWithEmission(vertex, lightEmission), normal.x(), normal.y(), normal.z());
            }

            return;
        }

        byte normalX = normalIntValue(normal.x());
        byte normalY = normalIntValue(normal.y());
        byte normalZ = normalIntValue(normal.z());
        int  overlay = instance.overlayCoords();

        for (int vertex = 0; vertex < QuadVertices.COUNT; vertex ++) {
            Vector3fc position = quad.position(vertex);
            long      packedUv = quad.packedUV(vertex);
            int       light    = instance.getLightCoordsWithEmission(vertex, lightEmission);

            QuadVertices.putPosition(pointer, matrix, position.x(), position.y(), position.z());
            putRgba(pointer + 12L, instance.getColor(vertex));
            MemoryUtil.memPutFloat(pointer + 16L, UVPair.unpackU(packedUv));
            MemoryUtil.memPutFloat(pointer + 20L, UVPair.unpackV(packedUv));

            if (this.entityFormat) {
                putPackedUv(pointer + 24L, overlay);
                putPackedUv(pointer + 28L, light);
                MemoryUtil.memPutByte(pointer + 32L, normalX);
                MemoryUtil.memPutByte(pointer + 33L, normalY);
                MemoryUtil.memPutByte(pointer + 34L, normalZ);
            } else {
                putPackedUv(pointer + 24L, light);
            }

            pointer += this.vertexSize;
        }
    }
}
