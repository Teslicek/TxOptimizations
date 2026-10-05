package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.CompactVectorArray;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StagedVertexBuffer.class)
public abstract class StagedVertexBufferCentroidPoolMixin {

    @Unique
    private static final int MAX_POOLED_SIZES = 256;

    @Unique
    private static final Int2ObjectOpenHashMap<CompactVectorArray> CENTROIDS = new Int2ObjectOpenHashMap<>();

    @Redirect(method = "decodeSortingPoints", at = @At(value = "NEW", target = "(I)Lcom/mojang/blaze3d/vertex/CompactVectorArray;"))
    private static CompactVectorArray txoptimizations$pooledCentroids(int count) {
        CompactVectorArray centroids = CENTROIDS.get(count);

        if (centroids != null) {
            Arrays.fill(((CompactVectorArrayAccessor) centroids).txoptimizations$contents(), 0.0F);

            return centroids;
        }

        if (CENTROIDS.size() == MAX_POOLED_SIZES)
            CENTROIDS.clear();

        centroids = new CompactVectorArray(count);
        CENTROIDS.put(count, centroids);

        return centroids;
    }
}
