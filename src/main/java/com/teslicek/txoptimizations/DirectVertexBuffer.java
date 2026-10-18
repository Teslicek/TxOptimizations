package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.vertex.VertexFormat;

public interface DirectVertexBuffer {

    VertexFormat txoptimizations$format();

    long txoptimizations$tryReserveVertices(VertexFormat format, int count);

    boolean txoptimizations$duplicatesVertices();

    long txoptimizations$reserveAfterLastVertex(int count);
}
