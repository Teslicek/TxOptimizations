package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.pipeline.PipelineCache;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PipelineCache.class)
public interface PipelineCacheAccessor {

    @Accessor("cache")
    Map<RenderPipeline, CompiledRenderPipeline> txoptimizations$cache();
}
