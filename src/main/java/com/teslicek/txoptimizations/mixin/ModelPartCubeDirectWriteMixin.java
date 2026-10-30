package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.teslicek.txoptimizations.CubeVertices;
import com.teslicek.txoptimizations.DirectVertexBuffer;
import com.teslicek.txoptimizations.GpuCube;
import com.teslicek.txoptimizations.GpuCubes;
import com.teslicek.txoptimizations.NormalCache;
import com.teslicek.txoptimizations.PositionTransform;
import com.teslicek.txoptimizations.VertexBytes;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = ModelPart.Cube.class, priority = 999)
public abstract class ModelPartCubeDirectWriteMixin implements GpuCube {

    @Shadow
    @Final
    public ModelPart.Polygon[] polygons;

    @Unique
    private ModelPart.@Nullable Polygon txoptimizations$face0;

    @Unique
    private ModelPart.@Nullable Polygon txoptimizations$face1;

    @Unique
    private ModelPart.@Nullable Polygon txoptimizations$face2;

    @Unique
    private ModelPart.@Nullable Polygon txoptimizations$face3;

    @Unique
    private ModelPart.@Nullable Polygon txoptimizations$face4;

    @Unique
    private ModelPart.@Nullable Polygon txoptimizations$face5;

    @Unique
    private float @Nullable [] txoptimizations$values;

    @Unique
    private boolean txoptimizations$flattenedBox;

    @Unique
    private float @Nullable [] txoptimizations$gpuValues;

    @Unique
    private int txoptimizations$gpuIndex;

    @Unique
    private int txoptimizations$gpuGeneration;

    @Overwrite
    public void compile(PoseStack.Pose pose, VertexConsumer builder, int lightCoords, int overlayCoords, int color) {
        if (NormalCache.usable() && this.txoptimizations$tryPut(pose, builder, lightCoords, overlayCoords, color))
            return;

        Matrix4f matrix        = pose.pose();
        Vector3f scratchVector = new Vector3f();

        for (ModelPart.Polygon polygon : this.polygons) {
            Vector3f normal = pose.transformNormal(polygon.normal(), scratchVector);
            float    nx     = normal.x();
            float    ny     = normal.y();
            float    nz     = normal.z();

            for (ModelPart.Vertex vertex : polygon.vertices()) {
                Vector3f position = matrix.transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), scratchVector);

                builder.addVertex(position.x(), position.y(), position.z(), color, vertex.u(), vertex.v(), overlayCoords, lightCoords, nx, ny, nz);
            }
        }
    }

    @Override
    public int txoptimizations$gpuIndex() {
        float[] values = this.txoptimizations$values;

        if (values == null || !this.txoptimizations$flattenedBox)
            throw new IllegalStateException("A cube without flattened values was sent to the GPU");

        if (values != this.txoptimizations$gpuValues || this.txoptimizations$gpuGeneration != GpuCubes.generation()) {
            this.txoptimizations$gpuIndex      = GpuCubes.register(values);
            this.txoptimizations$gpuValues     = values;
            this.txoptimizations$gpuGeneration = GpuCubes.generation();
        }

        return this.txoptimizations$gpuIndex;
    }

    @Unique
    private float @Nullable [] txoptimizations$values() {
        ModelPart.Polygon[] polygons = this.polygons;

        if (this.txoptimizations$flattenedBox && polygons[0] == this.txoptimizations$face0 && polygons[1] == this.txoptimizations$face1 && polygons[2] == this.txoptimizations$face2 && polygons[3] == this.txoptimizations$face3 && polygons[4] == this.txoptimizations$face4 && polygons[5] == this.txoptimizations$face5)
            return this.txoptimizations$values;

        float[] values = CubeVertices.flatten(polygons);

        this.txoptimizations$face0        = polygons[0];
        this.txoptimizations$face1        = polygons[1];
        this.txoptimizations$face2        = polygons[2];
        this.txoptimizations$face3        = polygons[3];
        this.txoptimizations$face4        = polygons[4];
        this.txoptimizations$face5        = polygons[5];
        this.txoptimizations$values       = values;
        this.txoptimizations$flattenedBox = true;

        return values;
    }

    @Unique
    private boolean txoptimizations$tryPut(PoseStack.Pose pose, VertexConsumer builder, int lightCoords, int overlayCoords, int color) {
        float[] values = this.polygons.length == CubeVertices.FACES ? this.txoptimizations$values() : null;

        if (values != null)
            return GpuCubes.tryAdd(pose, this, builder, lightCoords, overlayCoords, color) || CubeVertices.tryPut(pose, values, builder, lightCoords, overlayCoords, color);

        int vertices = 0;

        for (ModelPart.Polygon polygon : this.polygons)
            vertices += polygon.vertices().length;

        long pointer = vertices > 0 && builder instanceof DirectVertexBuffer direct ? direct.txoptimizations$tryReserveVertices(DefaultVertexFormat.ENTITY, vertices) : MemoryUtil.NULL;

        if (pointer == MemoryUtil.NULL)
            return false;

        Matrix4f matrix     = pose.pose();
        int      properties = matrix.properties();

        NormalCache.select(pose);

        for (ModelPart.Polygon polygon : this.polygons) {
            int normal = NormalCache.packed(pose, polygon.normal());

            for (ModelPart.Vertex vertex : polygon.vertices()) {
                float x = vertex.worldX();
                float y = vertex.worldY();
                float z = vertex.worldZ();

                VertexBytes.putEntity(pointer, PositionTransform.x(matrix, properties, x, y, z), PositionTransform.y(matrix, properties, x, y, z), PositionTransform.z(matrix, properties, x, y, z), color, vertex.u(), vertex.v(), overlayCoords, lightCoords, normal);
                pointer += VertexBytes.ENTITY_STRIDE;
            }
        }

        return true;
    }
}
