package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.teslicek.txoptimizations.OutlineModelView;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderType.class)
public abstract class RenderTypeOutlineModelViewMixin {

    @Shadow
    public abstract boolean isOutline();

    @WrapOperation(method = "prepare", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderType;writeDynamicTransforms(Lorg/joml/Matrix4f;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"))
    private GpuBufferSlice txoptimizations$keepOutlineModelView(RenderType renderType, Matrix4f modelView, Operation<GpuBufferSlice> original, @Share("modelView") LocalRef<Matrix4f> kept) {
        GpuBufferSlice transforms = original.call(renderType, modelView);

        if (this.isOutline())
            kept.set(modelView);

        return transforms;
    }

    @ModifyReturnValue(method = "prepare", at = @At("RETURN"))
    private PreparedRenderType txoptimizations$attachOutlineModelView(PreparedRenderType prepared, @Share("modelView") LocalRef<Matrix4f> kept) {
        if (kept.get() != null)
            ((OutlineModelView) (Object) prepared).txoptimizations$setModelView(kept.get());

        return prepared;
    }
}
