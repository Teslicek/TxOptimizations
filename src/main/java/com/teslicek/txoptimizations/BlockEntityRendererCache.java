package com.teslicek.txoptimizations;

import java.util.Map;

public interface BlockEntityRendererCache {

    Map<?, ?> txoptimizations$getRendererSource();

    Object txoptimizations$getRenderer();

    void txoptimizations$setRenderer(Map<?, ?> rendererSource, Object renderer);
}
