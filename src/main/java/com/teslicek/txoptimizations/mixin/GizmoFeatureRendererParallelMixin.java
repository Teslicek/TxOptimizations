package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ParallelFeatures;
import net.minecraft.client.renderer.feature.GizmoFeatureRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GizmoFeatureRenderer.class)
public abstract class GizmoFeatureRendererParallelMixin {

    @Redirect(method = "buildGroup", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;getModelViewMatrixCopy()Lorg/joml/Matrix4f;"))
    private Matrix4f txoptimizations$modelView() {
        return ParallelFeatures.modelViewMatrixCopy();
    }
}
