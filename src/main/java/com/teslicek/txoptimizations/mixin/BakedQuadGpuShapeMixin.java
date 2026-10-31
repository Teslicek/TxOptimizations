package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.GpuCube;
import com.teslicek.txoptimizations.GpuCubes;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BakedQuad.class)
public abstract class BakedQuadGpuShapeMixin implements GpuCube {

    @Unique
    private int txoptimizations$gpuIndex;

    @Unique
    private int txoptimizations$gpuGeneration = -1;

    @Override
    public int txoptimizations$gpuIndex() {
        if (this.txoptimizations$gpuGeneration != GpuCubes.generation()) {
            this.txoptimizations$gpuIndex      = GpuCubes.registerQuad((BakedQuad) (Object) this);
            this.txoptimizations$gpuGeneration = GpuCubes.generation();
        }

        return this.txoptimizations$gpuIndex;
    }
}
