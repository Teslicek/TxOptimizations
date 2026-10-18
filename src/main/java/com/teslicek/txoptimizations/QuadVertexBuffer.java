package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.vertex.VertexFormat;

public interface QuadVertexBuffer {

    VertexFormat txoptimizations$quadFormat();

    long txoptimizations$tryReserveQuad(VertexFormat format);
}
