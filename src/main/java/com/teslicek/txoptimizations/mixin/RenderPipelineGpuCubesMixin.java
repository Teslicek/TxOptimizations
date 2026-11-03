package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.teslicek.txoptimizations.GpuCubePipeline;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RenderPipeline.class)
public abstract class RenderPipelineGpuCubesMixin implements GpuCubePipeline {

    @Unique
    private int txoptimizations$cubeGeneration;

    @Unique
    private @Nullable RenderPipeline txoptimizations$cubeVariant;

    @Override
    public int txoptimizations$cubeGeneration() {
        return this.txoptimizations$cubeGeneration;
    }

    @Override
    public @Nullable RenderPipeline txoptimizations$cubeVariant() {
        return this.txoptimizations$cubeVariant;
    }

    @Override
    public void txoptimizations$setCubeVariant(int generation, @Nullable RenderPipeline variant) {
        this.txoptimizations$cubeGeneration = generation;
        this.txoptimizations$cubeVariant    = variant;
    }
}
