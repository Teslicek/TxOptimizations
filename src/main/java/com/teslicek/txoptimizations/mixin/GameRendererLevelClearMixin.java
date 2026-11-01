package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.teslicek.txoptimizations.RenderStateBuffers;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public abstract class GameRendererLevelClearMixin {

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/CommandEncoder;clearColorAndDepthTextures(Lcom/mojang/renderpearl/api/textures/GpuTexture;Lorg/joml/Vector4fc;Lcom/mojang/renderpearl/api/textures/GpuTexture;D)V"))
    private void txoptimizations$skipClearBeforeLevel(CommandEncoder encoder, GpuTexture colorTexture, Vector4fc clearColor, GpuTexture depthTexture, double clearDepth, Operation<Void> original) {
        if (RenderStateBuffers.renderTree().shouldRenderLevel)
            return;

        original.call(encoder, colorTexture, clearColor, depthTexture, clearDepth);
    }
}
