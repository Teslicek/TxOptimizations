package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.teslicek.txoptimizations.CompactTerrain;
import com.teslicek.txoptimizations.PackedTerrain;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChunkSectionsToRender.DrawIndirect.class, ChunkSectionsToRender.DrawSeparate.class})
public abstract class ChunkSectionsToRenderPackedTerrainMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 1)
    private void txoptimizations$skipUnreadableTerrain(ChunkSectionLayer layer, RenderPass renderPass, GpuBuffer defaultIndexBuffer, IndexType defaultIndexType, RenderPipeline renderPipelineOverride, RenderPipeline renderPipelineOverrideMultidraw, CallbackInfo ci) {
        if (CompactTerrain.active() && !PackedTerrain.isPackedSource())
            ci.cancel();
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;getCompiledPipeline(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;)Lcom/mojang/renderpearl/api/pipeline/CompiledRenderPipeline;"), require = 1)
    private RenderPipeline txoptimizations$compactTerrain(RenderPipeline pipeline) {
        return PackedTerrain.terrainPipeline(pipeline);
    }
}
