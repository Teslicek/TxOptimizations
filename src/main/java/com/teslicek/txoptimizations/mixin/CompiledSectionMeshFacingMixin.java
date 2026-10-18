package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.MeshData;
import com.teslicek.txoptimizations.FacingCulling;
import com.teslicek.txoptimizations.SectionFacing;
import com.teslicek.txoptimizations.SectionLayers;
import java.util.Map;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CompiledSectionMesh.class)
public abstract class CompiledSectionMeshFacingMixin implements SectionFacing {

    @Unique
    private final int[] @Nullable [] txoptimizations$facingRanges = new int[SectionLayers.VALUES.length][];

    @Inject(method = "<init>(Lnet/minecraft/client/renderer/chunk/TranslucencyPointOfView;Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;J)V", at = @At("RETURN"))
    private void txoptimizations$groupByFacing(TranslucencyPointOfView translucencyPointOfView, SectionCompiler.Results results, long compileTaskStartTimeNs, CallbackInfo ci) {
        for (Map.Entry<ChunkSectionLayer, MeshData> layer : results.renderedLayers.entrySet()) {
            if (layer.getKey().translucent())
                continue;

            this.txoptimizations$facingRanges[layer.getKey().ordinal()] = FacingCulling.groupQuads(layer.getValue());
        }
    }

    @Override
    public int @Nullable [] txoptimizations$facingRanges(ChunkSectionLayer layer) {
        return this.txoptimizations$facingRanges[layer.ordinal()];
    }
}
