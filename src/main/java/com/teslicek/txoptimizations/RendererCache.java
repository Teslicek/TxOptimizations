package com.teslicek.txoptimizations;

import java.util.Map;

public interface RendererCache {

    Map<?, ?> txoptimizations$getRendererSource();

    Object txoptimizations$getRenderer();

    void txoptimizations$setRenderer(Map<?, ?> rendererSource, Object renderer);
}
