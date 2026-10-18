package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.teslicek.txoptimizations.DirectVertexBuffer;
import com.teslicek.txoptimizations.TexturedVertexLayout;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
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
        if (!(builder instanceof DirectVertexBuffer direct))
            return true;

        TexturedVertexLayout layout = TexturedVertexLayout.glyph(direct.txoptimizations$format());

        if (layout == null)
            return true;

        float    x0             = x + this.left;
        float    x1             = x + this.right;
        float    y0             = y + this.up;
        float    y1             = y + this.down;
        float    shearY0        = italic ? 1.0F - 0.25F * this.up : 0.0F;
        float    shearY1        = italic ? 1.0F - 0.25F * this.down : 0.0F;
        float    extraThickness = bold ? 0.1F : 0.0F;
        long     pointer        = direct.txoptimizations$reserveVertices(VERTICES);
        Vector3f position       = new Vector3f();

        txoptimizations$put(pointer, layout, pose, position, x0 + shearY0 - extraThickness, y0 - extraThickness, z, color, this.u0, this.v0, light);
        txoptimizations$put(pointer + layout.stride(), layout, pose, position, x0 + shearY1 - extraThickness, y1 + extraThickness, z, color, this.u0, this.v1, light);
        txoptimizations$put(pointer + 2L * layout.stride(), layout, pose, position, x1 + shearY1 + extraThickness, y1 + extraThickness, z, color, this.u1, this.v1, light);
        txoptimizations$put(pointer + 3L * layout.stride(), layout, pose, position, x1 + shearY0 + extraThickness, y0 - extraThickness, z, color, this.u1, this.v0, light);

        return false;
    }

    @Unique
    private static void txoptimizations$put(long pointer, TexturedVertexLayout layout, Matrix4fc pose, Vector3f position, float x, float y, float z, int color, float u, float v, int light) {
        pose.transformPosition(x, y, z, position);
        layout.put(pointer, position.x(), position.y(), position.z(), color, u, v, light);
    }
}
