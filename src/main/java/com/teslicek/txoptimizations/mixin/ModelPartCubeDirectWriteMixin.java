package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.teslicek.txoptimizations.CubeVertices;
import com.teslicek.txoptimizations.DirectVertexBuffer;
import com.teslicek.txoptimizations.NormalCache;
import com.teslicek.txoptimizations.VertexBytes;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ModelPart.Cube.class)
public abstract class ModelPartCubeDirectWriteMixin {

    @Shadow
    @Final
    public ModelPart.Polygon[] polygons;

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

    @Unique
    private boolean txoptimizations$tryPut(PoseStack.Pose pose, VertexConsumer builder, int lightCoords, int overlayCoords, int color) {
        if (this.polygons.length == CubeVertices.FACES)
            return CubeVertices.tryPut(pose, this.polygons, builder, lightCoords, overlayCoords, color);

        int vertices = 0;

        for (ModelPart.Polygon polygon : this.polygons)
            vertices += polygon.vertices().length;

        long pointer = vertices > 0 && builder instanceof DirectVertexBuffer direct ? direct.txoptimizations$tryReserveVertices(DefaultVertexFormat.ENTITY, vertices) : MemoryUtil.NULL;

        if (pointer == MemoryUtil.NULL)
            return false;

        Matrix4f matrix        = pose.pose();
        Vector3f scratchVector = new Vector3f();

        NormalCache.select(pose);

        for (ModelPart.Polygon polygon : this.polygons) {
            int normal = NormalCache.packed(pose, polygon.normal());

            for (ModelPart.Vertex vertex : polygon.vertices()) {
                Vector3f position = matrix.transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), scratchVector);

                VertexBytes.putEntity(pointer, position.x(), position.y(), position.z(), color, vertex.u(), vertex.v(), overlayCoords, lightCoords, normal);
                pointer += VertexBytes.ENTITY_STRIDE;
            }
        }

        return true;
    }
}
