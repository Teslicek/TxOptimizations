package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import com.teslicek.txoptimizations.BakedQuadVertices;
import com.teslicek.txoptimizations.DirectVertexBuffer;
import com.teslicek.txoptimizations.GpuCubeBuilder;
import com.teslicek.txoptimizations.GpuCubeDraw;
import com.teslicek.txoptimizations.NormalCache;
import com.teslicek.txoptimizations.PositionTransform;
import com.teslicek.txoptimizations.RestartableBuilder;
import com.teslicek.txoptimizations.VertexFormatElementCache;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin implements DirectVertexBuffer, VertexConsumer, RestartableBuilder, GpuCubeBuilder {

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
    public int txoptimizations$vertexCount() {
        return this.vertices;
    }

    @Override
    public long txoptimizations$endAddress() {
        ByteBufferBuilderAccessor buffer = (ByteBufferBuilderAccessor) this.buffer;

        return buffer.txoptimizations$pointer() + buffer.txoptimizations$writeOffset();
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

    @Override
    public VertexConsumer addVertex(Matrix4fc pose, float x, float y, float z) {
        if (pose.getClass() != Matrix4f.class)
            return VertexConsumer.super.addVertex(pose, x, y, z);

        Matrix4f matrix     = (Matrix4f) pose;
        int      properties = matrix.properties();

        return this.addVertex(PositionTransform.x(matrix, properties, x, y, z), PositionTransform.y(matrix, properties, x, y, z), PositionTransform.z(matrix, properties, x, y, z));
    }

    @Shadow
    @Final
    private boolean blockFormat;

    @Shadow
    @Final
    private boolean entityFormat;

    @Override
    public void putBakedQuad(PoseStack.Pose pose, BakedQuad quad, QuadInstance instance) {
        if (this.entityFormat && this.txoptimizations$tryPutEntityQuad(pose, quad, instance))
            return;

        if (this.blockFormat && this.txoptimizations$tryPutBlockQuad(pose, quad, instance))
            return;

        Matrix4f matrix        = pose.pose();
        Vector3f normal        = pose.transformNormal(quad.direction().getUnitVec3f(), new Vector3f());
        int      lightEmission = quad.materialInfo().lightEmission();

        for (int vertex = 0; vertex < BakedQuadVertices.VERTICES; vertex ++) {
            Vector3f position = matrix.transformPosition(quad.position(vertex), new Vector3f());
            long     packedUv = quad.packedUV(vertex);

            this.addVertex(position.x(), position.y(), position.z(), instance.getColor(vertex), UVPair.unpackU(packedUv), UVPair.unpackV(packedUv), instance.overlayCoords(), instance.getLightCoordsWithEmission(vertex, lightEmission), normal.x(), normal.y(), normal.z());
        }
    }

    @Unique
    private boolean txoptimizations$tryPutEntityQuad(PoseStack.Pose pose, BakedQuad quad, QuadInstance instance) {
        long pointer = ((DirectVertexBuffer) this).txoptimizations$tryReserveVertices(this.format, BakedQuadVertices.VERTICES);

        if (pointer == MemoryUtil.NULL)
            return false;

        BakedQuadVertices.putEntity(pointer, this.vertexSize, pose, quad, instance, txoptimizations$packedNormal(pose, quad.direction()));

        return true;
    }

    @Unique
    private boolean txoptimizations$tryPutBlockQuad(PoseStack.Pose pose, BakedQuad quad, QuadInstance instance) {
        long pointer = ((DirectVertexBuffer) this).txoptimizations$tryReserveVertices(this.format, BakedQuadVertices.VERTICES);

        if (pointer == MemoryUtil.NULL)
            return false;

        BakedQuadVertices.putBlock(pointer, this.vertexSize, pose, quad, instance);

        return true;
    }

    @Unique
    private static int txoptimizations$packedNormal(PoseStack.Pose pose, Direction direction) {
        if (!NormalCache.usable())
            return NormalCache.pack(pose, direction.getUnitVec3f());

        NormalCache.select(pose);

        return NormalCache.packed(pose, direction);
    }

    @Unique
    private static final int POSITION = 0;

    @Shadow
    @Final
    private static String[] elementNames;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/vertex/VertexFormat;contains(Ljava/lang/String;)Z"))
    private boolean txoptimizations$cachedContains(VertexFormat format, String name) {
        return txoptimizations$element(format, name, POSITION) != null;
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/vertex/VertexFormat;getElement(Ljava/lang/String;)Lcom/mojang/renderpearl/api/vertex/VertexFormatElement;"))
    private VertexFormatElement txoptimizations$cachedElement(VertexFormat format, String name) {
        for (int index = 0; index < elementNames.length; index ++) {
            if (elementNames[index] == name)
                return txoptimizations$element(format, name, index);
        }

        throw new IllegalStateException("BufferBuilder asked for vertex element " + name + ", which is not in its name table");
    }

    @Unique
    private static VertexFormatElement txoptimizations$element(VertexFormat format, String name, int index) {
        if (elementNames[index] != name)
            throw new IllegalStateException("BufferBuilder asked for vertex element " + name + " at index " + index + ", where its name table has " + elementNames[index]);

        VertexFormatElementCache cache    = (VertexFormatElementCache) format;
        VertexFormatElement[]    elements = cache.txoptimizations$getBufferElements();

        if (elements == null) {
            elements = new VertexFormatElement[elementNames.length];

            for (int element = 0; element < elementNames.length; element ++)
                elements[element] = format.getElement(elementNames[element]);

            cache.txoptimizations$setBufferElements(elements);
        }

        return elements[index];
    }

    @Override
    public boolean txoptimizations$builds(VertexFormat format, PrimitiveTopology topology) {
        return this.format == format && this.primitiveTopology == topology;
    }

    @Override
    public void txoptimizations$restart() {
        if (this.building)
            throw new IllegalStateException("A buffer builder that is still building cannot be restarted");

        this.vertexPointer  = -1L;
        this.vertices       = 0;
        this.elementsToFill = 0;
        this.building       = true;
    }

    @Unique
    private @Nullable GpuCubeDraw txoptimizations$cubeDraw;

    @Override
    public @Nullable GpuCubeDraw txoptimizations$cubeDraw() {
        return this.txoptimizations$cubeDraw;
    }

    @Override
    public void txoptimizations$setCubeDraw(@Nullable GpuCubeDraw draw) {
        this.txoptimizations$cubeDraw = draw;
    }
}
