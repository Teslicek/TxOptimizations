package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teslicek.txoptimizations.RenderWorker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.gizmos.Gizmos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class MinecraftRenderWorkerMixin {

    @Shadow
    @Final
    public LevelRenderer levelRenderer;

    @WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render()V"), require = 1)
    private void txoptimizations$renderOnWorker(GameRenderer renderer, Operation<Void> original) {
        LevelRenderer levelRenderer = this.levelRenderer;

        RenderWorker.runAndWait(() -> {
            try (Gizmos.TemporaryCollection ignored = levelRenderer.collectPerFrameRenderThreadGizmos()) {
                original.call(renderer);
            }
        });
    }
}
