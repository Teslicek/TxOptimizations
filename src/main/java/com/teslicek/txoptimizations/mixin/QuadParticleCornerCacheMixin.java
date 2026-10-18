package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.teslicek.txoptimizations.DirectVertexBuffer;
import com.teslicek.txoptimizations.TexturedVertexLayout;
import java.util.Map;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(QuadParticleRenderState.class)
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

        if (count == 0)
            return;

        TexturedVertexLayout layout  = TexturedVertexLayout.PARTICLE;
        long                 pointer = builder instanceof DirectVertexBuffer direct ? direct.txoptimizations$tryReserveVertices(layout.format(), count * VERTICES) : MemoryUtil.NULL;

        if (pointer == MemoryUtil.NULL) {
            for (int particle = 0; particle < count; particle ++) {
                int f = particle * FLOATS_PER_PARTICLE;
                int i = particle * INTS_PER_PARTICLE;

                this.renderRotatedQuad(builder, floats[f], floats[f + 1], floats[f + 2], floats[f + 3], floats[f + 4], floats[f + 5], floats[f + 6], floats[f + 7], floats[f + 8], floats[f + 9], floats[f + 10], floats[f + 11], ints[i], ints[i + 1]);
            }

            return;
        }

        for (int particle = 0; particle < count; particle ++) {
            int f = particle * FLOATS_PER_PARTICLE;
            int i = particle * INTS_PER_PARTICLE;

            txoptimizations$rotateCorners(floats[f + 3], floats[f + 4], floats[f + 5], floats[f + 6]);
            txoptimizations$putVertex(pointer, layout, 0, floats[f], floats[f + 1], floats[f + 2], floats[f + 7], floats[f + 9], floats[f + 11], ints[i], ints[i + 1]);
            txoptimizations$putVertex(pointer + layout.stride(), layout, 1, floats[f], floats[f + 1], floats[f + 2], floats[f + 7], floats[f + 9], floats[f + 10], ints[i], ints[i + 1]);
            txoptimizations$putVertex(pointer + 2L * layout.stride(), layout, 2, floats[f], floats[f + 1], floats[f + 2], floats[f + 7], floats[f + 8], floats[f + 10], ints[i], ints[i + 1]);
            txoptimizations$putVertex(pointer + 3L * layout.stride(), layout, 3, floats[f], floats[f + 1], floats[f + 2], floats[f + 7], floats[f + 8], floats[f + 11], ints[i], ints[i + 1]);
            pointer += (long) VERTICES * layout.stride();
        }
    }

    @Unique
    private static void txoptimizations$rotateCorners(float x, float y, float z, float w) {
        int xBits = Float.floatToRawIntBits(x);
        int yBits = Float.floatToRawIntBits(y);
        int zBits = Float.floatToRawIntBits(z);
        int wBits = Float.floatToRawIntBits(w);

        if (cached && xBits == cachedX && yBits == cachedY && zBits == cachedZ && wBits == cachedW)
            return;

        ROTATION.set(x, y, z, w);

        for (int corner = 0; corner < VERTICES; corner ++) {
            VERTEX.set(CORNER_X[corner], CORNER_Y[corner], 0.0F).rotate(ROTATION);
            ROTATED[corner * 3]     = VERTEX.x;
            ROTATED[corner * 3 + 1] = VERTEX.y;
            ROTATED[corner * 3 + 2] = VERTEX.z;
        }

        cached  = true;
        cachedX = xBits;
        cachedY = yBits;
        cachedZ = zBits;
        cachedW = wBits;
    }

    @Unique
    private static void txoptimizations$putVertex(long pointer, TexturedVertexLayout layout, int corner, float x, float y, float z, float scale, float u, float v, int color, int light) {
        layout.put(pointer, ROTATED[corner * 3] * scale + x, ROTATED[corner * 3 + 1] * scale + y, ROTATED[corner * 3 + 2] * scale + z, color, u, v, light);
    }
}
