package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.teslicek.txoptimizations.GuiQuads;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlitRenderState.class)
public abstract class BlitRenderStateDirectWriteMixin {

    @Inject(method = "buildVertices", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$putQuad(VertexConsumer vertexConsumer, CallbackInfo ci) {
        BlitRenderState state = (BlitRenderState) (Object) this;

        if (GuiQuads.tryPutTextured(vertexConsumer, state.pose(), state.x0(), state.y0(), state.x1(), state.y1(), state.u0(), state.v0(), state.u1(), state.v1(), state.color()))
            ci.cancel();
    }
}
