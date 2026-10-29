package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class PipelineLayouts {

    private static final Map<List<Object>, Long> KEYS = new ConcurrentHashMap<>();
    private static final AtomicLong              NEXT = new AtomicLong();

    private PipelineLayouts() {
    }

    public static long key(List<BindGroupLayout.UniformDescription> uniforms, int pushConstantsSize) {
        List<Object> definition = new ArrayList<>(uniforms.size() + 1);

        for (BindGroupLayout.UniformDescription uniform : uniforms)
            definition.add(uniform.type());

        definition.add(pushConstantsSize);

        return KEYS.computeIfAbsent(definition, _ -> NEXT.incrementAndGet());
    }
}
