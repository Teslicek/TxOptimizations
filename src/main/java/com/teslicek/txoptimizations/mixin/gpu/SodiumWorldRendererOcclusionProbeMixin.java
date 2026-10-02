package com.teslicek.txoptimizations.mixin.gpu;

import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.teslicek.txoptimizations.gpu.OcclusionProbe;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.UniformBufferManager;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.oit.OitStage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SodiumWorldRenderer.class)
public abstract class SodiumWorldRendererOcclusionProbeMixin {

    @Shadow
    private RenderSectionManager renderSectionManager;

    @Shadow
    private UniformBufferManager uniformBufferManager;

    @Inject(method = "drawChunkLayer", at = @At("RETURN"))
    private void txoptimizations$probeOcclusion(RenderPass pass, ChunkSectionLayerGroup group, ChunkRenderMatrices matrices, double x, double y, double z, GpuSampler terrainSampler, OitStage stage, CallbackInfo ci) {
        if (group == ChunkSectionLayerGroup.OPAQUE && stage == null && OcclusionProbe.isActive())
            OcclusionProbe.onOpaqueTerrainDrawn(pass, this.renderSectionManager.getRenderLists(), this.uniformBufferManager.getUniformBuffer(), x, y, z);
    }
}
