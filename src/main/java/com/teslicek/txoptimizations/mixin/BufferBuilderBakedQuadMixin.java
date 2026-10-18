package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.teslicek.txoptimizations.BakedQuadVertices;
import com.teslicek.txoptimizations.DirectVertexBuffer;
import com.teslicek.txoptimizations.NormalCache;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderBakedQuadMixin implements VertexConsumer {

    @Shadow
    @Final
    private VertexFormat format;

    @Shadow
    @Final
    private boolean blockFormat;

    @Shadow
    @Final
    private boolean entityFormat;

    @Shadow
    @Final
    private int vertexSize;

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
}
