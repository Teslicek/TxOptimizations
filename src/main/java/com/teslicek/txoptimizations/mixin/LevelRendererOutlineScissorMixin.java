package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.teslicek.txoptimizations.OutlineScissor;
import java.util.Set;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererOutlineScissorMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void txoptimizations$beginOutlineFrame(CallbackInfo ci) {
        OutlineScissor.beginFrame();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ShaderManager;getPostChain(Lnet/minecraft/resources/Identifier;Ljava/util/Set;)Lnet/minecraft/client/renderer/PostChain;"))
    private PostChain txoptimizations$trackOutlineChain(ShaderManager shaderManager, Identifier id, Set<Identifier> targets) {
        PostChain chain = shaderManager.getPostChain(id, targets);

        OutlineScissor.useChain(chain);

        return chain;
    }

    @Redirect(method = "blitEntityOutline", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitAndBlendToTexture(Lcom/mojang/renderpearl/api/textures/GpuTextureView;Lcom/mojang/renderpearl/api/textures/GpuTextureView;)V"))
    private void txoptimizations$blendInsideOutline(RenderTarget target, GpuTextureView color, GpuTextureView depth) {
        OutlineScissor.beginBlit();
        target.blitAndBlendToTexture(color, depth);
        OutlineScissor.endBlit();
    }
}
