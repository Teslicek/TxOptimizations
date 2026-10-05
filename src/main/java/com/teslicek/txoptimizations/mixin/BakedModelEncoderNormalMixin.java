package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.teslicek.txoptimizations.DirectVertexBuffer;
import com.teslicek.txoptimizations.NormalCache;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.EntityVertex;
import net.caffeinemc.mods.sodium.client.model.quad.BakedQuadView;
import net.caffeinemc.mods.sodium.client.render.immediate.model.BakedModelEncoder;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = BakedModelEncoder.class, priority = 1100)
public abstract class BakedModelEncoderNormalMixin {

    @Unique
    private static final int VERTICES = 4;

    @Shadow
    @Final
    private static boolean USE_COLOR_MULTIPLICATION;

    @Overwrite
    public static void writeQuadVertices(VertexBufferWriter writer, Pose matrices, BakedQuadView quad, QuadInstance instance) {
        if (writer instanceof DirectVertexBuffer direct && direct.txoptimizations$writesFormat(EntityVertex.FORMAT)) {
            txoptimizations$putQuad(direct.txoptimizations$reserveVertices(VERTICES), matrices, quad, instance);
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(VERTICES * EntityVertex.STRIDE);

            txoptimizations$putQuad(buffer, matrices, quad, instance);
            writer.push(stack, buffer, VERTICES, EntityVertex.FORMAT);
        }
    }

    @Unique
    private static void txoptimizations$putQuad(long buffer, Pose matrices, BakedQuadView quad, QuadInstance instance) {
        Matrix3f matNormal       = matrices.normal();
        Matrix4f matPosition     = matrices.pose();
        boolean  trustedNormals  = ((PoseAccessor) (Object) matrices).txoptimizations$trustedNormals();
        boolean  cached          = NormalCache.usable();
        int      lastNormal      = 0;
        int      lastTransformed = 0;
        boolean  hasNormal       = false;
        long     ptr             = buffer;

        if (cached)
            NormalCache.select(matNormal, trustedNormals);

        for (int i = 0; i < VERTICES; i ++) {
            float x        = quad.getX(i);
            float y        = quad.getY(i);
            float z        = quad.getZ(i);
            int   newLight = instance.getLightCoordsWithEmission(i, quad.getLightEmission());
            int   color    = instance.getColor(i);

            if (USE_COLOR_MULTIPLICATION)
                color = ColorMixer.mulComponentWise(color, quad.getColor(i));

            int normal = quad.getAccurateNormal(i);

            if (!hasNormal || normal != lastNormal) {
                lastNormal      = normal;
                lastTransformed = cached ? NormalCache.transform(matNormal, normal) : MatrixHelper.transformNormal(matNormal, trustedNormals, normal);
                hasNormal       = true;
            }

            float xt = MatrixHelper.transformPositionX(matPosition, x, y, z);
            float yt = MatrixHelper.transformPositionY(matPosition, x, y, z);
            float zt = MatrixHelper.transformPositionZ(matPosition, x, y, z);

            EntityVertex.write(ptr, xt, yt, zt, ColorARGB.toABGR(color), quad.getTexU(i), quad.getTexV(i), instance.overlayCoords(), newLight, lastTransformed);
            ptr += EntityVertex.STRIDE;
        }
    }
}
