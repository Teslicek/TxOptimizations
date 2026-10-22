package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.teslicek.txoptimizations.PositionTransform;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderPoseVertexMixin implements VertexConsumer {

    @Override
    public VertexConsumer addVertex(Matrix4fc pose, float x, float y, float z) {
        if (pose.getClass() != Matrix4f.class)
            return VertexConsumer.super.addVertex(pose, x, y, z);

        Matrix4f matrix     = (Matrix4f) pose;
        int      properties = matrix.properties();

        return this.addVertex(PositionTransform.x(matrix, properties, x, y, z), PositionTransform.y(matrix, properties, x, y, z), PositionTransform.z(matrix, properties, x, y, z));
    }
}
