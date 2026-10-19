package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.teslicek.txoptimizations.OutlineScissor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderTarget.class)
public abstract class RenderTargetOutlineScissorMixin {

    @WrapOperation(method = "blitAndBlendToTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/RenderPass;draw(IIII)V"))
    private void txoptimizations$blendInsideOutline(RenderPass renderPass, int vertexCount, int instanceCount, int firstVertex, int firstInstance, Operation<Void> original) {
        if (!OutlineScissor.isBlitting()) {
            original.call(renderPass, vertexCount, instanceCount, firstVertex, firstInstance);
            return;
        }

        RenderTarget target = (RenderTarget) (Object) this;

        if (OutlineScissor.isEmpty(target.width, target.height))
            return;

        OutlineScissor.enableScissor(renderPass, target.width, target.height);
        original.call(renderPass, vertexCount, instanceCount, firstVertex, firstInstance);
    }
}
