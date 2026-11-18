package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.GpuCube;
import com.teslicek.txoptimizations.GpuCubes;
import com.teslicek.txoptimizations.ItemShapes;
import com.teslicek.txoptimizations.ItemShapesSlot;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BakedQuad.class)
public abstract class BakedQuadGpuShapeMixin implements GpuCube, ItemShapesSlot {

    @Unique
    private int txoptimizations$gpuIndex;

    @Unique
    private int txoptimizations$gpuGeneration = -1;

    @Unique
    private @Nullable ItemShapes txoptimizations$itemShapes;

    @Override
    public @Nullable ItemShapes txoptimizations$itemShapes() {
        return this.txoptimizations$itemShapes;
    }

    @Override
    public void txoptimizations$setItemShapes(ItemShapes shapes) {
        this.txoptimizations$itemShapes = shapes;
    }

    @Override
    public int txoptimizations$gpuIndex() {
        if (this.txoptimizations$gpuGeneration != GpuCubes.generation()) {
            this.txoptimizations$gpuIndex      = GpuCubes.registerQuad((BakedQuad) (Object) this);
            this.txoptimizations$gpuGeneration = GpuCubes.generation();
        }

        return this.txoptimizations$gpuIndex;
    }
}
