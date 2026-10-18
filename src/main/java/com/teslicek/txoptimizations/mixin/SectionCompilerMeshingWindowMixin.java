package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.teslicek.txoptimizations.BiomeBlendWindow;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SectionCompiler.class)
public abstract class SectionCompilerMeshingWindowMixin {

    @WrapMethod(method = "compile")
    private SectionCompiler.Results txoptimizations$compileInWindows(SectionPos sectionPos, RenderSectionRegion region, VertexSorting vertexSorting, SectionBufferBuilderPack builders, Operation<SectionCompiler.Results> original) {
        BiomeBlendWindow.begin(((RenderSectionRegionAccessor) region).txoptimizations$level(), sectionPos);

        try {
            return original.call(sectionPos, region, vertexSorting, builders);
        } finally {
            BiomeBlendWindow.end();
        }
    }
}
