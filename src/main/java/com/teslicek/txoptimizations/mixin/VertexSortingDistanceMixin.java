package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.VertexSorting;
import com.teslicek.txoptimizations.DistanceSorting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = VertexSorting.class, priority = 999)
public interface VertexSortingDistanceMixin {

    @Overwrite
    static VertexSorting byDistance(VertexSorting.DistanceFunction function) {
        return new DistanceSorting(function);
    }
}
