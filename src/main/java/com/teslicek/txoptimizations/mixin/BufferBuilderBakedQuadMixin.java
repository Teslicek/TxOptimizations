package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.teslicek.txoptimizations.DirectVertexBuffer;
import com.teslicek.txoptimizations.NormalCache;
import com.teslicek.txoptimizations.VertexBytes;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderBakedQuadMixin implements VertexConsumer {

    @Unique
    private static final int VERTICES = 4;

    @Shadow
    @Final
    private VertexFormat format;

    @Override
    public void putBakedQuad(PoseStack.Pose pose, BakedQuad quad, QuadInstance instance) {
        if (this.format != DefaultVertexFormat.ENTITY || !NormalCache.usable()) {
            VertexConsumer.super.putBakedQuad(pose, quad, instance);

            return;
        }

        NormalCache.select(pose);

        Matrix4f matrix        = pose.pose();
        Vector3f scratchVector = new Vector3f();
        int      normal        = NormalCache.packed(pose, quad.direction());
        int      lightEmission = quad.materialInfo().lightEmission();
        long     pointer       = ((DirectVertexBuffer) this).txoptimizations$reserveVertices(VERTICES);

        for (int vertex = 0; vertex < VERTICES; vertex ++) {
            long     packedUv = quad.packedUV(vertex);
            Vector3f position = matrix.transformPosition(quad.position(vertex), scratchVector);

            VertexBytes.putEntity(pointer, position.x(), position.y(), position.z(), instance.getColor(vertex), UVPair.unpackU(packedUv), UVPair.unpackV(packedUv), instance.overlayCoords(), instance.getLightCoordsWithEmission(vertex, lightEmission), normal);
            pointer += VertexBytes.ENTITY_STRIDE;
        }
    }
}
