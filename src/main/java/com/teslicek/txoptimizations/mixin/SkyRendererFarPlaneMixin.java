package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.teslicek.txoptimizations.SkyFarPlane;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.minecraft.client.renderer.SkyRenderer;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SkyRenderer.class)
public abstract class SkyRendererFarPlaneMixin {

    @ModifyExpressionValue(method = "*", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderPipelines;SKY:Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;"))
    private RenderPipeline txoptimizations$farSky(RenderPipeline pipeline) {
        return SkyFarPlane.isDrawing() ? SkyFarPlane.SKY : pipeline;
    }

    @ModifyExpressionValue(method = "*", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderPipelines;SUNRISE_SUNSET:Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;"))
    private RenderPipeline txoptimizations$farSunrise(RenderPipeline pipeline) {
        return SkyFarPlane.isDrawing() ? SkyFarPlane.SUNRISE_SUNSET : pipeline;
    }

    @ModifyExpressionValue(method = "*", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderPipelines;STARS:Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;"))
    private RenderPipeline txoptimizations$farStars(RenderPipeline pipeline) {
        return SkyFarPlane.isDrawing() ? SkyFarPlane.STARS : pipeline;
    }

    @ModifyExpressionValue(method = "*", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderPipelines;CELESTIAL:Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;"))
    private RenderPipeline txoptimizations$farCelestial(RenderPipeline pipeline) {
        return SkyFarPlane.isDrawing() ? SkyFarPlane.CELESTIAL : pipeline;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/CommandEncoder;createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Ljava/util/Optional;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/renderpearl/api/commands/RenderPass;"))
    private RenderPass txoptimizations$drawInMainPass(CommandEncoder encoder, Supplier<String> label, GpuTextureView color, Optional<Vector4fc> clearColor, GpuTextureView depth, OptionalDouble clearDepth) {
        if (!SkyFarPlane.isDrawing())
            return encoder.createRenderPass(label, color, clearColor, depth, clearDepth);

        return SkyFarPlane.hostPass(color, depth);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/RenderPass;close()V"))
    private void txoptimizations$keepMainPassOpen(RenderPass pass) {
        if (SkyFarPlane.isDrawing())
            return;

        pass.close();
    }
}
