package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.teslicek.txoptimizations.GlyphQuads;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BakedSheetGlyph.class)
public abstract class BakedSheetGlyphQuadMixin {

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
    private float shearTop() {
        throw new AssertionError();
    }

    @Shadow
    private float shearBottom() {
        throw new AssertionError();
    }

    @Shadow
    private static float extraThickness(boolean bold) {
        throw new AssertionError();
    }

    @Overwrite
    private void render(boolean italic, float x, float y, float z, Matrix4fc pose, VertexConsumer builder, int color, boolean bold, int packedLightCoords) {
        float x0             = x + this.left;
        float x1             = x + this.right;
        float y0             = y + this.up;
        float y1             = y + this.down;
        float shearY0        = italic ? this.shearTop() : 0.0F;
        float shearY1        = italic ? this.shearBottom() : 0.0F;
        float extraThickness = extraThickness(bold);
        float topLeftX       = x0 + shearY0 - extraThickness;
        float bottomLeftX    = x0 + shearY1 - extraThickness;
        float bottomRightX   = x1 + shearY1 + extraThickness;
        float topRightX      = x1 + shearY0 + extraThickness;
        float topY           = y0 - extraThickness;
        float bottomY        = y1 + extraThickness;

        if (GlyphQuads.tryWrite(builder, pose, topLeftX, topY, bottomLeftX, bottomY, bottomRightX, bottomY, topRightX, topY, z, color, this.u0, this.v0, this.u1, this.v1, packedLightCoords))
            return;

        builder.addVertex(pose, topLeftX, topY, z).setColor(color).setUv(this.u0, this.v0).setLight(packedLightCoords);
        builder.addVertex(pose, bottomLeftX, bottomY, z).setColor(color).setUv(this.u0, this.v1).setLight(packedLightCoords);
        builder.addVertex(pose, bottomRightX, bottomY, z).setColor(color).setUv(this.u1, this.v1).setLight(packedLightCoords);
        builder.addVertex(pose, topRightX, topY, z).setColor(color).setUv(this.u1, this.v0).setLight(packedLightCoords);
    }

    @Overwrite
    private void buildEffect(BakedSheetGlyph.EffectInstance effect, float offset, float z, int color, VertexConsumer buffer, int packedLightCoords, Matrix4fc pose) {
        float x0 = effect.x0() + offset;
        float x1 = effect.x1() + offset;
        float y0 = effect.y0() + offset;
        float y1 = effect.y1() + offset;

        if (GlyphQuads.tryWrite(buffer, pose, x0, y1, x1, y1, x1, y0, x0, y0, z, color, this.u0, this.v0, this.u1, this.v1, packedLightCoords))
            return;

        buffer.addVertex(pose, x0, y1, z).setColor(color).setUv(this.u0, this.v0).setLight(packedLightCoords);
        buffer.addVertex(pose, x1, y1, z).setColor(color).setUv(this.u0, this.v1).setLight(packedLightCoords);
        buffer.addVertex(pose, x1, y0, z).setColor(color).setUv(this.u1, this.v1).setLight(packedLightCoords);
        buffer.addVertex(pose, x0, y0, z).setColor(color).setUv(this.u1, this.v0).setLight(packedLightCoords);
    }
}
