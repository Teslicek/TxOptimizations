package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.teslicek.txoptimizations.OutlineScissor;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PostChain.class)
public abstract class PostChainOutlineDepthMixin {

    @ModifyExpressionValue(method = "addToFrame", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/resource/RenderTargetDescriptor$TextureProperties;DEFAULT_DEPTH:Lcom/mojang/blaze3d/resource/RenderTargetDescriptor$TextureProperties;"))
    private RenderTargetDescriptor.TextureProperties txoptimizations$dropOutlineDepth(RenderTargetDescriptor.TextureProperties depth) {
        if (OutlineScissor.isOutlineChain((PostChain) (Object) this))
            return null;

        return depth;
    }
}
