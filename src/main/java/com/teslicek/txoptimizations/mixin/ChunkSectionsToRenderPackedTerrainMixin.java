package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.teslicek.txoptimizations.PackedTerrain;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkSectionsToRender.DrawIndirect.class)
public abstract class ChunkSectionsToRenderPackedTerrainMixin {

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;pipeline(Z)Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;"), require = 1)
    private RenderPipeline txoptimizations$packedTerrain(RenderPipeline vanilla, @Local(argsOnly = true) ChunkSectionLayer layer) {
        return PackedTerrain.multiDrawPipeline(layer, vanilla);
    }
}
