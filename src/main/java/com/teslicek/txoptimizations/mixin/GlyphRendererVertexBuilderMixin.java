package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.renderer.feature.TextFeatureRenderer$GlyphRenderer")
public abstract class GlyphRendererVertexBuilderMixin {

    @Unique
    private RenderType txoptimizations$lastRenderType;

    @Unique
    private VertexConsumer txoptimizations$lastVertexBuilder;

    @Redirect(method = "acceptRenderable", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/TextFeatureRenderer;getVertexBuilder(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer txoptimizations$reuseVertexBuilder(TextFeatureRenderer renderer, RenderType renderType) {
        if (this.txoptimizations$lastRenderType == renderType)
            return this.txoptimizations$lastVertexBuilder;

        this.txoptimizations$lastRenderType    = renderType;
        this.txoptimizations$lastVertexBuilder = renderer.getVertexBuilder(renderType);

        return this.txoptimizations$lastVertexBuilder;
    }
}
