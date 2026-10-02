package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.DirectVertexWriter;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ParticleVertex;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = QuadParticleRenderState.class, priority = 1100)
public abstract class QuadParticleDirectWriteMixin {

    @Unique
    private static final Vector3f CORNER = new Vector3f();

    @Overwrite
    private void sodium$emitVertices(VertexBufferWriter writer, float x, float y, float z, float size, float u0, float u1, float v0, float v1, int color, int light, Quaternionf quaternion) {
        long pointer = ((DirectVertexWriter) writer).txoptimizations$reserveVertices(4, ParticleVertex.FORMAT);

        txoptimizations$putCorner(pointer, 1.0F, -1.0F, x, y, z, size, u1, v1, color, light, quaternion);
        txoptimizations$putCorner(pointer + ParticleVertex.STRIDE, 1.0F, 1.0F, x, y, z, size, u1, v0, color, light, quaternion);
        txoptimizations$putCorner(pointer + 2L * ParticleVertex.STRIDE, -1.0F, 1.0F, x, y, z, size, u0, v0, color, light, quaternion);
        txoptimizations$putCorner(pointer + 3L * ParticleVertex.STRIDE, -1.0F, -1.0F, x, y, z, size, u0, v1, color, light, quaternion);
    }

    @Unique
    private static void txoptimizations$putCorner(long pointer, float cornerX, float cornerY, float x, float y, float z, float size, float u, float v, int color, int light, Quaternionf quaternion) {
        CORNER.set(cornerX, cornerY, 0.0F).rotate(quaternion).mul(size).add(x, y, z);
        ParticleVertex.put(pointer, CORNER.x, CORNER.y, CORNER.z, u, v, color, light);
    }
}
