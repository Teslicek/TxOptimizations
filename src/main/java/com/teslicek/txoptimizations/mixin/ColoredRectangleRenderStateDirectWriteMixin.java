package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.teslicek.txoptimizations.GuiQuads;
import net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ColoredRectangleRenderState.class)
public abstract class ColoredRectangleRenderStateDirectWriteMixin {

    @Inject(method = "buildVertices", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$putQuad(VertexConsumer vertexConsumer, CallbackInfo ci) {
        ColoredRectangleRenderState state = (ColoredRectangleRenderState) (Object) this;

        if (GuiQuads.tryPutColored(vertexConsumer, state.pose(), state.x0(), state.y0(), state.x1(), state.y1(), state.col1(), state.col2()))
            ci.cancel();
    }
}
