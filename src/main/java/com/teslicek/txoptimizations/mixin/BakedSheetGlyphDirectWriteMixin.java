package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.teslicek.txoptimizations.DirectVertexBuffer;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.GlyphVertex;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BakedSheetGlyph.class)
public abstract class BakedSheetGlyphDirectWriteMixin {

    @Unique
    private static final int VERTICES = 4;

    @Shadow
    @Final
    private float left;

    @Shadow
    @Final
    private float right;

    @Shadow
    @Final
    private float up;

    @Shadow
    @Final
    private float down;

    @Shadow
    @Final
    private float u0;

    @Shadow
    @Final
    private float u1;

    @Shadow
    @Final
    private float v0;

    @Shadow
    @Final
    private float v1;

    @WrapWithCondition(method = "renderChar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/font/glyphs/BakedSheetGlyph;render(ZFFFLorg/joml/Matrix4fc;Lcom/mojang/blaze3d/vertex/VertexConsumer;IZI)V"))
    private boolean txoptimizations$writeGlyphDirectly(BakedSheetGlyph glyph, boolean italic, float x, float y, float z, Matrix4fc pose, VertexConsumer builder, int color, boolean bold, int light) {
        if (builder instanceof DirectVertexBuffer direct && direct.txoptimizations$writesFormat(GlyphVertex.FORMAT)) {
            this.txoptimizations$writeQuad(direct.txoptimizations$reserveVertices(VERTICES), italic, x, y, z, pose, color, bold, light);

            return false;
        }

        VertexBufferWriter writer = VertexBufferWriter.tryOf(builder);

        if (writer == null)
            return true;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(VERTICES * GlyphVertex.STRIDE);

            this.txoptimizations$writeQuad(buffer, italic, x, y, z, pose, color, bold, light);
            writer.push(stack, buffer, VERTICES, GlyphVertex.FORMAT);
        }

        return false;
    }

    @Unique
    private void txoptimizations$writeQuad(long buffer, boolean italic, float x, float y, float z, Matrix4fc pose, int color, boolean bold, int light) {
        float x1     = x + this.left;
        float x2     = x + this.right;
        float h1     = y + this.up;
        float h2     = y + this.down;
        float w1     = italic ? 1.0F - 0.25F * this.up : 0.0F;
        float w2     = italic ? 1.0F - 0.25F * this.down : 0.0F;
        float offset = bold ? 0.1F : 0.0F;
        int   abgr   = ColorARGB.toABGR(color);

        txoptimizations$put(buffer, pose, x1 + w1 - offset, h1 - offset, z, abgr, this.u0, this.v0, light);
        txoptimizations$put(buffer + GlyphVertex.STRIDE, pose, x1 + w2 - offset, h2 + offset, z, abgr, this.u0, this.v1, light);
        txoptimizations$put(buffer + 2L * GlyphVertex.STRIDE, pose, x2 + w2 + offset, h2 + offset, z, abgr, this.u1, this.v1, light);
        txoptimizations$put(buffer + 3L * GlyphVertex.STRIDE, pose, x2 + w1 + offset, h1 - offset, z, abgr, this.u1, this.v0, light);
    }

    @Unique
    private static void txoptimizations$put(long pointer, Matrix4fc pose, float x, float y, float z, int color, float u, float v, int light) {
        GlyphVertex.put(pointer, MatrixHelper.transformPositionX(pose, x, y, z), MatrixHelper.transformPositionY(pose, x, y, z), MatrixHelper.transformPositionZ(pose, x, y, z), color, u, v, light);
    }
}
