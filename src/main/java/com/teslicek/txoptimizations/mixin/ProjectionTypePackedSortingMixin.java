package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.teslicek.txoptimizations.PackedQuadSorting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ProjectionType.class)
public abstract class ProjectionTypePackedSortingMixin {

    @ModifyReturnValue(method = "vertexSorting", at = @At("RETURN"))
    private VertexSorting txoptimizations$packedSorting(VertexSorting sorting) {
        if (sorting == VertexSorting.DISTANCE_TO_ORIGIN)
            return PackedQuadSorting.DISTANCE_TO_ORIGIN;

        if (sorting == VertexSorting.ORTHOGRAPHIC_Z)
            return PackedQuadSorting.ORTHOGRAPHIC_Z;

        throw new IllegalStateException("Projection type " + this + " sorts quads with an unknown vertex sorting " + sorting);
    }
}
