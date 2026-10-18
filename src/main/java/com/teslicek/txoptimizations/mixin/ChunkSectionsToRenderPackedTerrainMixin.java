package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.teslicek.txoptimizations.PackedTerrain;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkSectionsToRender.DrawIndirect.class)
public abstract class ChunkSectionsToRenderPackedTerrainMixin {

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;pipeline(Z)Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;"))
    private RenderPipeline txoptimizations$packedTerrain(ChunkSectionLayer layer, boolean multiDraw, Operation<RenderPipeline> original) {
        RenderPipeline vanilla = original.call(layer, multiDraw);

        if (!multiDraw)
            throw new IllegalStateException("Indirect terrain draws asked for the " + layer.label() + " pipeline without multi-draw");

        return PackedTerrain.multiDrawPipeline(layer, vanilla);
    }
}
