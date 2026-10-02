package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.vertex.VertexFormat;

public interface DirectVertexBuffer {

    boolean txoptimizations$writesFormat(VertexFormat format);

    long txoptimizations$reserveVertices(int count);

    VertexFormat txoptimizations$format();

    boolean txoptimizations$duplicatesVertices();

    long txoptimizations$reserveAfterLastVertex(int count);
}
