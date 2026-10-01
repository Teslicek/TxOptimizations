package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.teslicek.txoptimizations.MapAtlas;
import com.teslicek.txoptimizations.MapAtlasRenderState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.MapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMapAtlasMixin {

    @WrapOperation(method = "map", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;innerBlit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Lcom/mojang/renderpearl/api/textures/GpuSampler;IIIIFFFFI)V", ordinal = 0))
    private void txoptimizations$blitFromAtlas(GuiGraphicsExtractor graphics, RenderPipeline pipeline, GpuTextureView textureView, GpuSampler sampler, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1, int color, Operation<Void> original, @Local(name = "mapRenderState", argsOnly = true) MapRenderState mapRenderState) {
        MapAtlasRenderState state = (MapAtlasRenderState) mapRenderState;

        if (state.txoptimizations$getAtlasTexture() == null || !state.txoptimizations$getAtlasTexture().equals(mapRenderState.texture)) {
            original.call(graphics, pipeline, textureView, sampler, x0, y0, x1, y1, u0, u1, v0, v1, color);

            return;
        }

        float atlasU = state.txoptimizations$getAtlasU();
        float atlasV = state.txoptimizations$getAtlasV();

        original.call(graphics, pipeline, textureView, sampler, x0, y0, x1, y1, atlasU, atlasU + MapAtlas.MAP_UV, atlasV, atlasV + MapAtlas.MAP_UV, color);
    }
}
