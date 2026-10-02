package com.teslicek.txoptimizations.mixin;

import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ParticleVertex;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = QuadParticleRenderState.class, priority = 1100)
public abstract class QuadParticleCornerCacheMixin {

    @Unique
    private static final int VERTICES = 4;

    @Unique
    private static final float[] CORNER_X = {1.0F, 1.0F, -1.0F, -1.0F};

    @Unique
    private static final float[] CORNER_Y = {-1.0F, 1.0F, 1.0F, -1.0F};

    @Unique
    private static final float[] ROTATED = new float[VERTICES * 3];

    @Unique
    private static final Vector3f VERTEX = new Vector3f();

    @Unique
    private static boolean cached;

    @Unique
    private static int cachedX;

    @Unique
    private static int cachedY;

    @Unique
    private static int cachedZ;

    @Unique
    private static int cachedW;

    @Overwrite
    private void sodium$emitVertices(VertexBufferWriter writer, float x, float y, float z, float size, float u0, float u1, float v0, float v1, int color, int light, Quaternionf quaternion) {
        txoptimizations$rotateCorners(quaternion);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(VERTICES * ParticleVertex.STRIDE);

            txoptimizations$putVertex(buffer, 0, x, y, z, size, u1, v1, color, light);
            txoptimizations$putVertex(buffer + ParticleVertex.STRIDE, 1, x, y, z, size, u1, v0, color, light);
            txoptimizations$putVertex(buffer + 2L * ParticleVertex.STRIDE, 2, x, y, z, size, u0, v0, color, light);
            txoptimizations$putVertex(buffer + 3L * ParticleVertex.STRIDE, 3, x, y, z, size, u0, v1, color, light);
            writer.push(stack, buffer, VERTICES, ParticleVertex.FORMAT);
        }
    }

    @Unique
    private static void txoptimizations$rotateCorners(Quaternionf quaternion) {
        int x = Float.floatToRawIntBits(quaternion.x);
        int y = Float.floatToRawIntBits(quaternion.y);
        int z = Float.floatToRawIntBits(quaternion.z);
        int w = Float.floatToRawIntBits(quaternion.w);

        if (cached && x == cachedX && y == cachedY && z == cachedZ && w == cachedW)
            return;

        for (int corner = 0; corner < VERTICES; corner ++) {
            VERTEX.set(CORNER_X[corner], CORNER_Y[corner], 0.0F).rotate(quaternion);
            ROTATED[corner * 3]     = VERTEX.x;
            ROTATED[corner * 3 + 1] = VERTEX.y;
            ROTATED[corner * 3 + 2] = VERTEX.z;
        }

        cached  = true;
        cachedX = x;
        cachedY = y;
        cachedZ = z;
        cachedW = w;
    }

    @Unique
    private static void txoptimizations$putVertex(long pointer, int corner, float x, float y, float z, float size, float u, float v, int color, int light) {
        VERTEX.set(ROTATED[corner * 3], ROTATED[corner * 3 + 1], ROTATED[corner * 3 + 2]).mul(size).add(x, y, z);
        ParticleVertex.put(pointer, VERTEX.x, VERTEX.y, VERTEX.z, u, v, color, light);
    }
}
