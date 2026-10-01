package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.CompactVectorArray;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.renderpearl.api.pipeline.IndexType;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MeshData.SortState.class)
public abstract class MeshDataSortStateIndexMixin {

    @Shadow
    @Final
    private CompactVectorArray centroids;

    @Shadow
    @Final
    private IndexType indexType;

    @Inject(method = "writeSortedIndexBuffer", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$writeSortedIndicesDirectly(ByteBuffer buffer, VertexSorting sorting, CallbackInfo ci) {
        int[] quadOrder = sorting.sort(this.centroids);

        if (this.indexType == IndexType.SHORT) {
            ShortBuffer indices = buffer.asShortBuffer();

            for (int quad : quadOrder) {
                int first = quad * 4;

                indices.put((short) first);
                indices.put((short) (first + 1));
                indices.put((short) (first + 2));
                indices.put((short) (first + 2));
                indices.put((short) (first + 3));
                indices.put((short) first);
            }

            buffer.position(buffer.position() + indices.position() * Short.BYTES);
            ci.cancel();
            return;
        }

        if (this.indexType == IndexType.INT) {
            IntBuffer indices = buffer.asIntBuffer();

            for (int quad : quadOrder) {
                int first = quad * 4;

                indices.put(first);
                indices.put(first + 1);
                indices.put(first + 2);
                indices.put(first + 2);
                indices.put(first + 3);
                indices.put(first);
            }

            buffer.position(buffer.position() + indices.position() * Integer.BYTES);
            ci.cancel();
            return;
        }

        throw new IllegalStateException("Unknown index type " + this.indexType);
    }
}
