package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.vertex.VertexFormat;

public interface DirectVertexBuffer {

    VertexFormat txoptimizations$format();

    int txoptimizations$vertexCount();

    long txoptimizations$tryReserveVertices(VertexFormat format, int count);

    boolean txoptimizations$duplicatesVertices();

    long txoptimizations$reserveAfterLastVertex(int count);
}
