package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.teslicek.txoptimizations.SkyFarPlane;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

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
}
