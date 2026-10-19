package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.teslicek.txoptimizations.OutlineModelView;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(RenderType.class)
public abstract class RenderTypeOutlineModelViewMixin {

    @Unique
    private static Matrix4f txoptimizations$outlineModelView;

    @Shadow
    public abstract boolean isOutline();

    @ModifyArg(method = "writeDynamicTransforms", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DynamicGpuData;writeTransform(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"), index = 0)
    private Matrix4f txoptimizations$keepOutlineModelView(Matrix4f modelView) {
        if (this.isOutline())
            txoptimizations$outlineModelView = modelView;

        return modelView;
    }

    @ModifyReturnValue(method = "prepare", at = @At("RETURN"))
    private PreparedRenderType txoptimizations$attachOutlineModelView(PreparedRenderType prepared) {
        if (!this.isOutline())
            return prepared;

        if (txoptimizations$outlineModelView == null)
            throw new IllegalStateException("Outline render type was prepared without writing its model-view matrix");

        ((OutlineModelView) (Object) prepared).txoptimizations$setModelView(txoptimizations$outlineModelView);
        txoptimizations$outlineModelView = null;

        return prepared;
    }
}
