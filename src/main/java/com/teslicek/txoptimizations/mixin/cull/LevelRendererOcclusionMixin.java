package com.teslicek.txoptimizations.mixin.cull;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.teslicek.txoptimizations.cull.DepthReadback;
import com.teslicek.txoptimizations.cull.OcclusionCuller;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererOcclusionMixin {

    @WrapOperation(method = "lambda$addMainPass$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;executeClassicTransparency(Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;Lcom/mojang/renderpearl/api/commands/RenderPass;)V"))
    private void txoptimizations$captureOpaqueDepth(LevelRenderer renderer, ChunkSectionsToRender sections, FeatureRenderDispatcher.PreparedFrame frame, RenderPass pass, Operation<Void> original) {
        if (!DepthReadback.canCapture()) {
            original.call(renderer, sections, frame, pass);

            return;
        }

        RenderTarget mainTarget = Minecraft.getInstance().gameRenderer.mainRenderTarget;

        pass.close();
        DepthReadback.capture(mainTarget);

        try (RenderPass translucent = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Main", mainTarget.getColorTextureView(), Optional.empty(), mainTarget.getDepthTextureView(), OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(translucent);
            original.call(renderer, sections, frame, translucent);
        }
    }

    @Inject(method = "setLevel", at = @At("TAIL"))
    private void txoptimizations$resetOcclusion(CallbackInfo ci) {
        OcclusionCuller.reset();
    }
}
