package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemFeatureRenderer.class)
public abstract class ItemFeatureVertexBuilderMixin {

    @Unique
    private ItemFeatureRenderer.Submit txoptimizations$lastSubmit;

    @Unique
    private RenderType txoptimizations$lastRenderType;

    @Unique
    private VertexConsumer txoptimizations$lastVertexBuilder;

    @Redirect(method = "prepareMainSubmit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer;getVertexBuilder(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer txoptimizations$reuseVertexBuilder(ItemFeatureRenderer renderer, RenderType renderType, ItemFeatureRenderer.Submit submit) {
        if (submit == this.txoptimizations$lastSubmit && renderType == this.txoptimizations$lastRenderType && renderType.canConsolidateConsecutiveGeometry())
            return this.txoptimizations$lastVertexBuilder;

        VertexConsumer builder = renderer.getVertexBuilder(renderType);

        this.txoptimizations$lastSubmit        = submit;
        this.txoptimizations$lastRenderType    = renderType;
        this.txoptimizations$lastVertexBuilder = builder;

        return builder;
    }
}
