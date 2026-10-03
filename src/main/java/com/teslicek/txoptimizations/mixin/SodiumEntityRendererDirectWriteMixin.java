package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.teslicek.txoptimizations.DirectVertexBuffer;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.EntityVertex;
import net.caffeinemc.mods.sodium.client.render.immediate.model.EntityRenderer;
import net.caffeinemc.mods.sodium.client.render.immediate.model.ModelCuboid;
import net.minecraft.core.Direction;
import org.joml.Matrix3f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = EntityRenderer.class, remap = false)
public abstract class SodiumEntityRendererDirectWriteMixin {

    @Unique
    private static final int FACE_VERTICES = 4;

    @Unique
    private static final int FACES = 6;

    @Shadow
    private static void prepareVertices(Pose matrices, ModelCuboid cuboid, int color) {
        throw new AssertionError();
    }

    @Shadow
    @Final
    private static Matrix3f prevNormalMatrix;

    @Shadow
    @Final
    private static int[] CUBE_FACE_NORMAL;

    @Overwrite
    private static void prepareNormalsIfChanged(Pose matrices) {
        Matrix3f normal = matrices.normal();

        if (normal.equals(prevNormalMatrix))
            return;

        boolean trusted = ((PoseAccessor) (Object) matrices).txoptimizations$trustedNormals();
        int     up      = MatrixHelper.transformNormal(normal, trusted, Direction.UP);
        int     south   = MatrixHelper.transformNormal(normal, trusted, Direction.SOUTH);
        int     east    = MatrixHelper.transformNormal(normal, trusted, Direction.EAST);

        CUBE_FACE_NORMAL[0] = NormI8.flipPacked(up);
        CUBE_FACE_NORMAL[1] = up;
        CUBE_FACE_NORMAL[2] = east;
        CUBE_FACE_NORMAL[3] = NormI8.flipPacked(south);
        CUBE_FACE_NORMAL[4] = NormI8.flipPacked(east);
        CUBE_FACE_NORMAL[5] = south;
        prevNormalMatrix.set(normal);
    }

    @Shadow
    private static int emitQuads(long buffer, ModelCuboid cuboid, int overlay, int light) {
        throw new AssertionError();
    }

    @Overwrite
    public static void renderCuboid(Pose matrices, VertexBufferWriter writer, ModelCuboid cuboid, int light, int overlay, int color) {
        prepareVertices(matrices, cuboid, color);
        prepareNormalsIfChanged(matrices);

        if (writer instanceof DirectVertexBuffer direct && direct.txoptimizations$writesFormat(EntityVertex.FORMAT)) {
            int faces = 0;

            for (int face = 0; face < FACES; face ++) {
                if (cuboid.shouldDrawFace(face))
                    faces ++;
            }

            if (faces > 0)
                emitQuads(direct.txoptimizations$reserveVertices(faces * FACE_VERTICES), cuboid, overlay, light);

            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer   = stack.nmalloc(64, FACES * FACE_VERTICES * EntityVertex.STRIDE);
            int  vertices = emitQuads(buffer, cuboid, overlay, light);

            if (vertices > 0)
                writer.push(stack, buffer, vertices, EntityVertex.FORMAT);
        }
    }
}
