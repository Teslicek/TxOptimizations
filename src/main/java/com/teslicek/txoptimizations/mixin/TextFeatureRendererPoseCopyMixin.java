package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TextFeatureRenderer.class)
public abstract class TextFeatureRendererPoseCopyMixin {

    @Redirect(method = "buildGroup", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;set(Lorg/joml/Matrix4fc;)Lorg/joml/Matrix4f;"), require = 1)
    private Matrix4f txoptimizations$copyChangedPose(Matrix4f pose, Matrix4fc source, @Share("copiedPose") LocalRef<Matrix4fc> copied) {
        if (copied.get() == source)
            return pose;

        copied.set(source);

        return pose.set(source);
    }
}
