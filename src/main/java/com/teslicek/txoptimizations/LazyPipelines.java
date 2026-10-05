package com.teslicek.txoptimizations;

import com.mojang.blaze3d.pipeline.PipelineCache;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.teslicek.txoptimizations.mixin.PipelineCacheAccessor;
import com.teslicek.txoptimizations.mixin.RenderSystemPipelineCacheAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.client.renderer.RenderPipelines;

public final class LazyPipelines {

    private static volatile List<RenderPipeline> remembered = List.of();

    private LazyPipelines() {
    }

    public static void remember() {
        PipelineCache cache = RenderSystemPipelineCacheAccessor.txoptimizations$currentPipelineCache();

        if (cache == null) {
            remembered = List.of();

            return;
        }

        Set<RenderPipeline> registered = Collections.newSetFromMap(new IdentityHashMap<>());

        registered.addAll(RenderPipelines.requiredPipelines());
        registered.addAll(RenderPipelines.optionalPipelines());

        List<RenderPipeline> lazy = new ArrayList<>();

        for (RenderPipeline pipeline : ((PipelineCacheAccessor) cache).txoptimizations$cache().keySet()) {
            if (!registered.contains(pipeline))
                lazy.add(pipeline);
        }

        remembered = List.copyOf(lazy);
    }

    public static List<RenderPipeline> withRemembered(List<RenderPipeline> optional) {
        List<RenderPipeline> lazy = remembered;

        remembered = List.of();

        if (lazy.isEmpty())
            return optional;

        List<RenderPipeline> pipelines = new ArrayList<>(optional.size() + lazy.size());

        pipelines.addAll(optional);
        pipelines.addAll(lazy);

        return pipelines;
    }
}
