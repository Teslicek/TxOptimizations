package com.teslicek.txoptimizations;

import java.lang.reflect.Method;
import java.util.Arrays;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.joml.Options;
import org.joml.Quaternionf;

public interface ParticleSnapshot {

    int FLOATS    = 7;
    int INTS      = 3;
    int REFS      = 2;
    int O_ROLL    = 0;
    int ROLL      = 1;
    int SIZE      = 2;
    int U0        = 3;
    int U1        = 4;
    int V0        = 5;
    int V1        = 6;
    int FLAGS     = 0;
    int COLOR     = 1;
    int LIGHT     = 2;
    int LAYER     = 0;
    int FACING    = 1;
    int STORED    = 1;
    int OWN_SIZE  = 2;
    int OWN_LIGHT = 4;

    ClassValue<Integer> CLASS_FLAGS = new ClassValue<>() {

        @Override
        protected Integer computeValue(Class<?> type) {
            if (overrides(type, SingleQuadParticle.class, "extract", QuadParticleRenderState.class, Camera.class, float.class) || overrides(type, SingleQuadParticle.class, "extractRotatedQuad", QuadParticleRenderState.class, Camera.class, Quaternionf.class, float.class) || overrides(type, SingleQuadParticle.class, "extractRotatedQuad", QuadParticleRenderState.class, Quaternionf.class, float.class, float.class, float.class, float.class))
                return 0;

            int flags = STORED;

            if (overrides(type, SingleQuadParticle.class, "getQuadSize", float.class))
                flags |= OWN_SIZE;

            if (overrides(type, Particle.class, "getLightCoords", float.class))
                flags |= OWN_LIGHT;

            return flags;
        }
    };

    void txoptimizations$snapshot(float[] floats, int floatOffset, int[] ints, int intOffset, Object[] refs, int refOffset, double[] previous, int previousOffset);

    int txoptimizations$lightCoords(float partialTickTime);

    static Quaternionf rotateRoll(Quaternionf rotation, float angle) {
        if (angle != 0.0F || Options.FASTMATH)
            return rotation.rotateZ(angle);

        float sin = angle * 0.5F;
        float cos = 1.0F;

        return rotation.set(rotation.x * cos + rotation.y * sin, rotation.y * cos - rotation.x * sin, rotation.w * sin + rotation.z * cos, rotation.w * cos - rotation.z * sin);
    }

    private static boolean overrides(Class<?> type, Class<?> owner, String name, Class<?>... parameters) {
        for (Class<?> current = type; current != owner; current = current.getSuperclass()) {
            if (current == null)
                throw new IllegalStateException(type + " does not extend " + owner);

            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && Arrays.equals(method.getParameterTypes(), parameters))
                    return true;
            }
        }

        return false;
    }
}
