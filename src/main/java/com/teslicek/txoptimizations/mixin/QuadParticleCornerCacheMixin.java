package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.teslicek.txoptimizations.DirectVertexBuffer;
import com.teslicek.txoptimizations.TexturedVertexLayout;
import java.util.Map;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.joml.Math;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = QuadParticleRenderState.class, priority = 999)
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
    private static final int FLOATS_PER_PARTICLE = 12;

    @Unique
    private static final int INTS_PER_PARTICLE = 2;

    @Unique
    private static final int CACHED_LAYERS = 8;

    @Shadow
    @Final
    private Map<SingleQuadParticle.Layer, Object> particles;

    @Unique
    private final Object[] txoptimizations$builtLayers = new Object[CACHED_LAYERS];

    @Unique
    private final Object[] txoptimizations$builtStorages = new Object[CACHED_LAYERS];

    @Unique
    private int txoptimizations$builtCount;

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
        Object storage = this.txoptimizations$storage(layer);

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
    private Object txoptimizations$storage(SingleQuadParticle.Layer layer) {
        for (int index = 0; index < this.txoptimizations$builtCount; index ++) {
            if (this.txoptimizations$builtLayers[index] == layer)
                return this.txoptimizations$builtStorages[index];
        }

        Object storage = this.particles.get(layer);

        if (storage == null || this.txoptimizations$builtCount == CACHED_LAYERS)
            return storage;

        this.txoptimizations$builtLayers[this.txoptimizations$builtCount]   = layer;
        this.txoptimizations$builtStorages[this.txoptimizations$builtCount] = storage;
        this.txoptimizations$builtCount ++;

        return storage;
    }

    @Unique
    private static void txoptimizations$rotateCorners(float x, float y, float z, float w) {
        int xBits = Float.floatToRawIntBits(x);
        int yBits = Float.floatToRawIntBits(y);
        int zBits = Float.floatToRawIntBits(z);
        int wBits = Float.floatToRawIntBits(w);

        if (cached && xBits == cachedX && yBits == cachedY && zBits == cachedZ && wBits == cachedW)
            return;

        float xx  = x * x;
        float yy  = y * y;
        float zz  = z * z;
        float ww  = w * w;
        float xy  = x * y;
        float xz  = x * z;
        float yz  = y * z;
        float xw  = x * w;
        float zw  = z * w;
        float yw  = y * w;
        float k   = 1.0F / (xx + yy + zz + ww);
        float m00 = (xx - yy - zz + ww) * k;
        float m01 = 2.0F * (xy - zw) * k;
        float m02 = 2.0F * (xz + yw) * k;
        float m10 = 2.0F * (xy + zw) * k;
        float m11 = (yy - xx - zz + ww) * k;
        float m12 = 2.0F * (yz - xw) * k;
        float m20 = 2.0F * (xz - yw) * k;
        float m21 = 2.0F * (yz + xw) * k;
        float m22 = (zz - xx - yy + ww) * k;

        txoptimizations$rotateCorner(0, m00, m01, m02, m10, m11, m12, m20, m21, m22);
        txoptimizations$rotateCorner(1, m00, m01, m02, m10, m11, m12, m20, m21, m22);

        if (txoptimizations$negatable(0) && txoptimizations$negatable(1)) {
            for (int component = 0; component < 6; component ++)
                ROTATED[6 + component] = -ROTATED[component];
        } else {
            txoptimizations$rotateCorner(2, m00, m01, m02, m10, m11, m12, m20, m21, m22);
            txoptimizations$rotateCorner(3, m00, m01, m02, m10, m11, m12, m20, m21, m22);
        }

        cached  = true;
        cachedX = xBits;
        cachedY = yBits;
        cachedZ = zBits;
        cachedW = wBits;
    }

    @Unique
    private static void txoptimizations$rotateCorner(int corner, float m00, float m01, float m02, float m10, float m11, float m12, float m20, float m21, float m22) {
        float cornerX = CORNER_X[corner];
        float cornerY = CORNER_Y[corner];
        float cornerZ = 0.0F;

        ROTATED[corner * 3]     = Math.fma(m00, cornerX, Math.fma(m01, cornerY, m02 * cornerZ));
        ROTATED[corner * 3 + 1] = Math.fma(m10, cornerX, Math.fma(m11, cornerY, m12 * cornerZ));
        ROTATED[corner * 3 + 2] = Math.fma(m20, cornerX, Math.fma(m21, cornerY, m22 * cornerZ));
    }

    @Unique
    private static boolean txoptimizations$negatable(int corner) {
        for (int component = corner * 3; component < corner * 3 + 3; component ++) {
            float value = ROTATED[component];

            if (value != value || value == 0.0F)
                return false;
        }

        return true;
    }

    @Unique
    private static void txoptimizations$putVertex(long pointer, TexturedVertexLayout layout, int corner, float x, float y, float z, float scale, float u, float v, int color, int light) {
        layout.put(pointer, ROTATED[corner * 3] * scale + x, ROTATED[corner * 3 + 1] * scale + y, ROTATED[corner * 3 + 2] * scale + z, color, u, v, light);
    }
}
