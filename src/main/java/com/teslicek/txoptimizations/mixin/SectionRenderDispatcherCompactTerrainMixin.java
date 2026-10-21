package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.teslicek.txoptimizations.CompactTerrain;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SectionRenderDispatcher.class)
public abstract class SectionRenderDispatcherCompactTerrainMixin {

    @ModifyExpressionValue(method = "lambda$new$0", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/vertex/VertexFormat;getVertexSize()I"), require = 1)
    private int txoptimizations$compactAlignment(int vertexSize) {
        return CompactTerrain.bufferAlignment(vertexSize);
    }
}
