package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.vertex.VertexFormat;

public interface RestartableBuilder {

    boolean txoptimizations$builds(VertexFormat format, PrimitiveTopology topology);

    void txoptimizations$restart();
}
