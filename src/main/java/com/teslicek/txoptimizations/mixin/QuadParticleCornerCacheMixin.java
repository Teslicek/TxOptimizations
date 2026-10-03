package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.teslicek.txoptimizations.DirectVertexBuffer;
import java.util.Map;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ParticleVertex;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
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
    private static final Quaternionf ROTATION = new Quaternionf();

    @Unique
    private static final int FLOATS_PER_PARTICLE = 12;

    @Unique
    private static final int INTS_PER_PARTICLE = 2;

    @Shadow
    @Final
    private Map<SingleQuadParticle.Layer, Object> particles;

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

    @Shadow
    protected abstract void renderRotatedQuad(VertexConsumer builder, float x, float y, float z, float xRot, float yRot, float zRot, float wRot, float scale, float u0, float u1, float v0, float v1, int color, int lightCoords);

    @Overwrite
    public void buildLayer(SingleQuadParticle.Layer layer, VertexConsumer builder) {
        Object storage = this.particles.get(layer);

        if (storage == null)
            return;

        QuadParticleStorageAccessor stored = (QuadParticleStorageAccessor) storage;
        float[]                     floats = stored.txoptimizations$floatValues();
        int[]                       ints   = stored.txoptimizations$intValues();
        int                         count  = stored.txoptimizations$particleCount();

        if (!(builder instanceof DirectVertexBuffer direct) || !direct.txoptimizations$writesFormat(ParticleVertex.FORMAT)) {
            for (int particle = 0; particle < count; particle ++) {
                int f = particle * FLOATS_PER_PARTICLE;
                int i = particle * INTS_PER_PARTICLE;

                this.renderRotatedQuad(builder, floats[f], floats[f + 1], floats[f + 2], floats[f + 3], floats[f + 4], floats[f + 5], floats[f + 6], floats[f + 7], floats[f + 8], floats[f + 9], floats[f + 10], floats[f + 11], ints[i], ints[i + 1]);
            }

            return;
        }

        if (count == 0)
            return;

        long pointer = direct.txoptimizations$reserveVertices(count * VERTICES);

        for (int particle = 0; particle < count; particle ++) {
            int f = particle * FLOATS_PER_PARTICLE;
            int i = particle * INTS_PER_PARTICLE;

            txoptimizations$rotateCorners(ROTATION.set(floats[f + 3], floats[f + 4], floats[f + 5], floats[f + 6]));
            txoptimizations$putQuad(pointer, floats[f], floats[f + 1], floats[f + 2], floats[f + 7], floats[f + 8], floats[f + 9], floats[f + 10], floats[f + 11], ColorARGB.toABGR(ints[i]), ints[i + 1]);
            pointer += VERTICES * ParticleVertex.STRIDE;
        }
    }

    @Overwrite
    private void sodium$emitVertices(VertexBufferWriter writer, float x, float y, float z, float size, float u0, float u1, float v0, float v1, int color, int light, Quaternionf quaternion) {
        txoptimizations$rotateCorners(quaternion);

        if (writer instanceof DirectVertexBuffer direct && direct.txoptimizations$writesFormat(ParticleVertex.FORMAT)) {
            txoptimizations$putQuad(direct.txoptimizations$reserveVertices(VERTICES), x, y, z, size, u0, u1, v0, v1, color, light);
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(VERTICES * ParticleVertex.STRIDE);

            txoptimizations$putQuad(buffer, x, y, z, size, u0, u1, v0, v1, color, light);
            writer.push(stack, buffer, VERTICES, ParticleVertex.FORMAT);
        }
    }

    @Unique
    private static void txoptimizations$putQuad(long buffer, float x, float y, float z, float size, float u0, float u1, float v0, float v1, int color, int light) {
        txoptimizations$putVertex(buffer, 0, x, y, z, size, u1, v1, color, light);
        txoptimizations$putVertex(buffer + ParticleVertex.STRIDE, 1, x, y, z, size, u1, v0, color, light);
        txoptimizations$putVertex(buffer + 2L * ParticleVertex.STRIDE, 2, x, y, z, size, u0, v0, color, light);
        txoptimizations$putVertex(buffer + 3L * ParticleVertex.STRIDE, 3, x, y, z, size, u0, v1, color, light);
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
