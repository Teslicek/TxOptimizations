package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.teslicek.txoptimizations.OutlineScissor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderTarget.class)
public abstract class RenderTargetOutlineScissorMixin {

    @Redirect(method = "blitAndBlendToTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/RenderPass;draw(IIII)V"))
    private void txoptimizations$blendInsideOutline(RenderPass renderPass, int vertexCount, int instanceCount, int firstVertex, int firstInstance) {
        if (!OutlineScissor.isBlitting()) {
            renderPass.draw(vertexCount, instanceCount, firstVertex, firstInstance);
            return;
        }

        RenderTarget target = (RenderTarget) (Object) this;

        if (OutlineScissor.isEmpty(target.width, target.height))
            return;

        OutlineScissor.enableScissor(renderPass, target.width, target.height);
        renderPass.draw(vertexCount, instanceCount, firstVertex, firstInstance);
    }
}
