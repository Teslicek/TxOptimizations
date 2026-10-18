package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.VertexSorting;
import com.teslicek.txoptimizations.PackedQuadSorting;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class RenderSectionPackedSortingMixin {

    @Redirect(method = "createVertexSorting", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexSorting;byDistance(FFF)Lcom/mojang/blaze3d/vertex/VertexSorting;"))
    private VertexSorting txoptimizations$packedSorting(float x, float y, float z) {
        return PackedQuadSorting.byDistance(x, y, z);
    }
}
