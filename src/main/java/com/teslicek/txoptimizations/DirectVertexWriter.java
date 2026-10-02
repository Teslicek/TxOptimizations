package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.vertex.VertexFormat;

public interface DirectVertexWriter {

    long txoptimizations$reserveVertices(int count, VertexFormat format);
}
