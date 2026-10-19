package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.teslicek.txoptimizations.OutlineScissor;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PostPass.class)
public abstract class PostPassOutlineScissorMixin {

    @WrapOperation(method = "lambda$addToFrame$1", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/RenderPass;draw(IIII)V"))
    private void txoptimizations$drawInsideOutline(RenderPass renderPass, int vertexCount, int instanceCount, int firstVertex, int firstInstance, Operation<Void> original, @Local RenderTarget outputTarget) {
        if (!OutlineScissor.isOutlinePass((PostPass) (Object) this)) {
            original.call(renderPass, vertexCount, instanceCount, firstVertex, firstInstance);
            return;
        }

        if (OutlineScissor.isEmpty(outputTarget.width, outputTarget.height))
            return;

        OutlineScissor.enableScissor(renderPass, outputTarget.width, outputTarget.height);
        original.call(renderPass, vertexCount, instanceCount, firstVertex, firstInstance);
    }
}
