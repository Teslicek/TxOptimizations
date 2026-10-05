package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.pipeline.PipelineCache;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSystem.class)
public interface RenderSystemPipelineCacheAccessor {

    @Accessor("currentPipelineCache")
    static PipelineCache txoptimizations$currentPipelineCache() {
        throw new AssertionError();
    }
}
