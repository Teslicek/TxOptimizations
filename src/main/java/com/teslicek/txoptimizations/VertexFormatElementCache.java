package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.vertex.VertexFormatElement;

public interface VertexFormatElementCache {

    VertexFormatElement[] txoptimizations$getBufferElements();

    void txoptimizations$setBufferElements(VertexFormatElement[] elements);
}
