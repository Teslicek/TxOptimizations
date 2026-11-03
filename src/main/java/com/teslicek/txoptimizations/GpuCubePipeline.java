package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import org.jspecify.annotations.Nullable;

public interface GpuCubePipeline {

    int txoptimizations$cubeGeneration();

    @Nullable RenderPipeline txoptimizations$cubeVariant();

    void txoptimizations$setCubeVariant(int generation, @Nullable RenderPipeline variant);
}
