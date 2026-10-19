package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.teslicek.txoptimizations.DirectVertexBuffer;
import com.teslicek.txoptimizations.VertexBytes;
import net.minecraft.client.renderer.feature.ShadowFeatureRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import org.joml.Math;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = ShadowFeatureRenderer.class, priority = 999)
public abstract class ShadowFeatureRendererDirectMixin {

    @Unique
    private static final int VERTICES = 4;

    @Unique
    private static final int FULL_BRIGHT = 15728880;

    @Unique
    private static final int UP_NORMAL = VertexBytes.packNormal(0.0F, 1.0F, 0.0F);

    @Shadow
    private static void shadowVertex(Matrix4fc pose, VertexConsumer buffer, int color, float x, float y, float z, float u, float v) {
        throw new AssertionError();
    }

    @Overwrite
    private void prepare(ShadowFeatureRenderer.Submit submit, VertexConsumer builder) {
        for (EntityRenderState.ShadowPiece piece : submit.pieces()) {
            AABB  aabb   = piece.shapeBelow().bounds();
            float x01    = piece.relativeX() + (float) aabb.minX;
            float x11    = piece.relativeX() + (float) aabb.maxX;
            float y01    = piece.relativeY() + (float) aabb.minY;
            float z01    = piece.relativeZ() + (float) aabb.minZ;
            float z11    = piece.relativeZ() + (float) aabb.maxZ;
            float radius = submit.radius();
            float u0     = -x01 / 2.0F / radius + 0.5F;
            float u1     = -x11 / 2.0F / radius + 0.5F;
            float v0     = -z01 / 2.0F / radius + 0.5F;
            float v1     = -z11 / 2.0F / radius + 0.5F;
            int   color  = ARGB.white(piece.alpha());

            if (txoptimizations$tryPut(builder, submit.pose(), color, x01, x11, y01, z01, z11, u0, u1, v0, v1))
                continue;

            shadowVertex(submit.pose(), builder, color, x01, y01, z01, u0, v0);
            shadowVertex(submit.pose(), builder, color, x01, y01, z11, u0, v1);
            shadowVertex(submit.pose(), builder, color, x11, y01, z11, u1, v1);
            shadowVertex(submit.pose(), builder, color, x11, y01, z01, u1, v0);
        }
    }

    @Unique
    private static boolean txoptimizations$tryPut(VertexConsumer builder, Matrix4fc pose, int color, float x0, float x1, float y, float z0, float z1, float u0, float u1, float v0, float v1) {
        if (!(builder instanceof DirectVertexBuffer direct) || direct.txoptimizations$format() != DefaultVertexFormat.ENTITY)
            return false;

        long pointer = direct.txoptimizations$tryReserveVertices(DefaultVertexFormat.ENTITY, VERTICES);

        if (pointer == MemoryUtil.NULL)
            return false;

        long second     = pointer + VertexBytes.ENTITY_STRIDE;
        long third      = second + VertexBytes.ENTITY_STRIDE;
        long fourth     = third + VertexBytes.ENTITY_STRIDE;
        int  properties = pose.properties();

        if ((properties & Matrix4fc.PROPERTY_IDENTITY) != 0) {
            txoptimizations$put(pointer, x0, y, z0, color, u0, v0);
            txoptimizations$put(second, x0, y, z1, color, u0, v1);
            txoptimizations$put(third, x1, y, z1, color, u1, v1);
            txoptimizations$put(fourth, x1, y, z0, color, u1, v0);

            return true;
        }

        if ((properties & Matrix4fc.PROPERTY_TRANSLATION) != 0) {
            float left  = x0 + pose.m30();
            float right = x1 + pose.m30();
            float level = y + pose.m31();
            float near  = z0 + pose.m32();
            float far   = z1 + pose.m32();

            txoptimizations$put(pointer, left, level, near, color, u0, v0);
            txoptimizations$put(second, left, level, far, color, u0, v1);
            txoptimizations$put(third, right, level, far, color, u1, v1);
            txoptimizations$put(fourth, right, level, near, color, u1, v0);

            return true;
        }

        float nearX = Math.fma(pose.m10(), y, Math.fma(pose.m20(), z0, pose.m30()));
        float nearY = Math.fma(pose.m11(), y, Math.fma(pose.m21(), z0, pose.m31()));
        float nearZ = Math.fma(pose.m12(), y, Math.fma(pose.m22(), z0, pose.m32()));
        float farX  = Math.fma(pose.m10(), y, Math.fma(pose.m20(), z1, pose.m30()));
        float farY  = Math.fma(pose.m11(), y, Math.fma(pose.m21(), z1, pose.m31()));
        float farZ  = Math.fma(pose.m12(), y, Math.fma(pose.m22(), z1, pose.m32()));

        txoptimizations$put(pointer, Math.fma(pose.m00(), x0, nearX), Math.fma(pose.m01(), x0, nearY), Math.fma(pose.m02(), x0, nearZ), color, u0, v0);
        txoptimizations$put(second, Math.fma(pose.m00(), x0, farX), Math.fma(pose.m01(), x0, farY), Math.fma(pose.m02(), x0, farZ), color, u0, v1);
        txoptimizations$put(third, Math.fma(pose.m00(), x1, farX), Math.fma(pose.m01(), x1, farY), Math.fma(pose.m02(), x1, farZ), color, u1, v1);
        txoptimizations$put(fourth, Math.fma(pose.m00(), x1, nearX), Math.fma(pose.m01(), x1, nearY), Math.fma(pose.m02(), x1, nearZ), color, u1, v0);

        return true;
    }

    @Unique
    private static void txoptimizations$put(long pointer, float x, float y, float z, int color, float u, float v) {
        VertexBytes.putEntity(pointer, x, y, z, color, u, v, OverlayTexture.NO_OVERLAY, FULL_BRIGHT, UP_NORMAL);
    }
}
