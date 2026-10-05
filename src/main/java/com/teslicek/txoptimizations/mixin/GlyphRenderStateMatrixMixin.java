package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.renderer.state.gui.GlyphRenderState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GlyphRenderState.class)
public abstract class GlyphRenderStateMatrixMixin {

    @Unique
    private static final Matrix4f SCRATCH = new Matrix4f();

    @Redirect(method = "buildVertices", at = @At(value = "NEW", target = "()Lorg/joml/Matrix4f;"))
    private Matrix4f txoptimizations$reuseMatrix() {
        return SCRATCH.identity();
    }
}
