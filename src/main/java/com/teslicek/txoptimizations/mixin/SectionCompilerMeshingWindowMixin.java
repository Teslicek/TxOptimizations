package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.teslicek.txoptimizations.BiomeBlendWindow;
import com.teslicek.txoptimizations.SectionLightWindow;
import com.teslicek.txoptimizations.SectionStateAccess;
import com.teslicek.txoptimizations.SectionStateWindow;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SectionCompiler.class)
public abstract class SectionCompilerMeshingWindowMixin {

    @WrapMethod(method = "compile")
    private SectionCompiler.Results txoptimizations$compileInWindows(SectionPos sectionPos, RenderSectionRegion region, VertexSorting vertexSorting, SectionBufferBuilderPack builders, Operation<SectionCompiler.Results> original) {
        SectionLightWindow lightWindow = (SectionLightWindow) BlockModelLighter.CACHE.get();
        SectionStateAccess states      = (SectionStateAccess) region;
        SectionStateWindow stateWindow = SectionStateWindow.begin(sectionPos);

        BiomeBlendWindow.begin(((RenderSectionRegionAccessor) region).txoptimizations$level(), sectionPos);
        lightWindow.txoptimizations$beginSection(sectionPos.minBlockX() - 1, sectionPos.minBlockY() - 1, sectionPos.minBlockZ() - 1);
        states.txoptimizations$windowStates(stateWindow);

        try {
            return original.call(sectionPos, region, vertexSorting, builders);
        } finally {
            BiomeBlendWindow.end();
            lightWindow.txoptimizations$endSection();
            states.txoptimizations$windowStates(null);
            stateWindow.end();
        }
    }

    @Redirect(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState txoptimizations$loopBlockState(RenderSectionRegion region, BlockPos pos) {
        return ((SectionStateAccess) region).txoptimizations$directBlockState(pos);
    }
}
