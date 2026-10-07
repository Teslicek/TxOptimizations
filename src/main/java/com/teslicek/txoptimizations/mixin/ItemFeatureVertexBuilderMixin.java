package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFeatureRenderer.class)
public abstract class ItemFeatureVertexBuilderMixin {

    @Unique
    private RenderType txoptimizations$lastRenderType;

    @Unique
    private VertexConsumer txoptimizations$lastVertexBuilder;

    @Inject(method = "prepareMainSubmit", at = @At("HEAD"))
    private void txoptimizations$forgetVertexBuilder(ItemFeatureRenderer.Submit submit, CallbackInfo ci) {
        this.txoptimizations$lastRenderType    = null;
        this.txoptimizations$lastVertexBuilder = null;
    }

    @Redirect(method = "prepareMainSubmit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer;getVertexBuilder(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer txoptimizations$reuseVertexBuilder(ItemFeatureRenderer renderer, RenderType renderType) {
        if (renderType == this.txoptimizations$lastRenderType && renderType.canConsolidateConsecutiveGeometry())
            return this.txoptimizations$lastVertexBuilder;

        VertexConsumer builder = renderer.getVertexBuilder(renderType);

        this.txoptimizations$lastRenderType    = renderType;
        this.txoptimizations$lastVertexBuilder = builder;

        return builder;
    }
}
