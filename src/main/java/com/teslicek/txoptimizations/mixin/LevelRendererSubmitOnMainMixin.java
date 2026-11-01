package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teslicek.txoptimizations.RenderStateBuffers;
import com.teslicek.txoptimizations.RenderWorker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererSubmitOnMainMixin {

    @Redirect(method = {"render", "submitFeatures"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;submitNodeStorage:Lnet/minecraft/client/renderer/SubmitNodeStorage;", opcode = Opcodes.GETFIELD), require = 5)
    private SubmitNodeStorage txoptimizations$storageForRole(LevelRenderer renderer) {
        return RenderStateBuffers.storageForRole();
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;submitFeatures(Lnet/minecraft/client/renderer/state/level/LevelRenderState;Lnet/minecraft/client/renderer/SubmitNodeCollector;Z)V"), require = 1)
    private void txoptimizations$submittedDuringExtraction(LevelRenderer renderer, LevelRenderState levelRenderState, SubmitNodeCollector collector, boolean renderBlockOutline, Operation<Void> original) {
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeStorage;setUseImprovedTransparency(Z)V"), require = 1)
    private void txoptimizations$transparencySetDuringExtraction(SubmitNodeStorage storage, boolean useImprovedTransparency, Operation<Void> original) {
    }

    @Inject(method = "frameHasAlwaysOnTopGizmos", at = @At("HEAD"), cancellable = true, require = 1)
    private void txoptimizations$renderFrameAlwaysOnTopGizmos(CallbackInfoReturnable<Boolean> cir) {
        if (RenderWorker.isCurrent())
            cir.setReturnValue(RenderStateBuffers.renderAlwaysOnTopGizmos());
    }
}
