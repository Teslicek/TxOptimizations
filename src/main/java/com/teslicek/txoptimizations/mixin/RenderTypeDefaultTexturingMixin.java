package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderType.class)
public abstract class RenderTypeDefaultTexturingMixin {

    @Unique
    private static final Matrix4f IDENTITY = new Matrix4f();

    @Redirect(method = "writeDynamicTransforms", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/TextureTransform;createMatrix()Lorg/joml/Matrix4f;"))
    private Matrix4f txoptimizations$sharedIdentity(TextureTransform transform) {
        return transform == TextureTransform.DEFAULT_TEXTURING ? IDENTITY : transform.createMatrix();
    }
}
