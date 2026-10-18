package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.BackgroundVertexCopy;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererVertexCopyMixin {

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;prepareFrame(Lnet/minecraft/client/renderer/SubmitNodeStorage;)Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;", shift = At.Shift.AFTER))
    private void txoptimizations$copyFeatureVerticesInBackground(CallbackInfo ci) {
        ((BackgroundVertexCopy) this.renderBuffers.stagedVertexBuffer()).txoptimizations$copyVerticesInBackground();
    }
}
