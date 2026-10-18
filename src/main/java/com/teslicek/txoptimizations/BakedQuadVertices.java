package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BakedQuadVertices {

    public static final int VERTICES = 4;

    private BakedQuadVertices() {
    }

    public static void putEntity(long pointer, int stride, PoseStack.Pose pose, BakedQuad quad, QuadInstance instance, int normal) {
        Matrix4f matrix        = pose.pose();
        Vector3f position      = new Vector3f();
        int      lightEmission = quad.materialInfo().lightEmission();
        int      overlay       = instance.overlayCoords();

        putEntity(pointer, matrix.transformPosition(quad.position0(), position), quad.packedUV0(), instance.getColor(0), overlay, instance.getLightCoordsWithEmission(0, lightEmission), normal);
        putEntity(pointer + stride, matrix.transformPosition(quad.position1(), position), quad.packedUV1(), instance.getColor(1), overlay, instance.getLightCoordsWithEmission(1, lightEmission), normal);
        putEntity(pointer + 2L * stride, matrix.transformPosition(quad.position2(), position), quad.packedUV2(), instance.getColor(2), overlay, instance.getLightCoordsWithEmission(2, lightEmission), normal);
        putEntity(pointer + 3L * stride, matrix.transformPosition(quad.position3(), position), quad.packedUV3(), instance.getColor(3), overlay, instance.getLightCoordsWithEmission(3, lightEmission), normal);
    }

    public static void putBlock(long pointer, int stride, PoseStack.Pose pose, BakedQuad quad, QuadInstance instance) {
        Matrix4f matrix        = pose.pose();
        Vector3f position      = new Vector3f();
        int      lightEmission = quad.materialInfo().lightEmission();

        putBlock(pointer, matrix.transformPosition(quad.position0(), position), quad.packedUV0(), instance.getColor(0), instance.getLightCoordsWithEmission(0, lightEmission));
        putBlock(pointer + stride, matrix.transformPosition(quad.position1(), position), quad.packedUV1(), instance.getColor(1), instance.getLightCoordsWithEmission(1, lightEmission));
        putBlock(pointer + 2L * stride, matrix.transformPosition(quad.position2(), position), quad.packedUV2(), instance.getColor(2), instance.getLightCoordsWithEmission(2, lightEmission));
        putBlock(pointer + 3L * stride, matrix.transformPosition(quad.position3(), position), quad.packedUV3(), instance.getColor(3), instance.getLightCoordsWithEmission(3, lightEmission));
    }

    private static void putEntity(long pointer, Vector3f position, long packedUv, int color, int overlay, int light, int normal) {
        VertexBytes.putEntity(pointer, position.x(), position.y(), position.z(), color, UVPair.unpackU(packedUv), UVPair.unpackV(packedUv), overlay, light, normal);
    }

    private static void putBlock(long pointer, Vector3f position, long packedUv, int color, int light) {
        VertexBytes.putBlock(pointer, position.x(), position.y(), position.z(), color, UVPair.unpackU(packedUv), UVPair.unpackV(packedUv), light);
    }
}
