package com.teslicek.txoptimizations;

public interface GuiLayerBounds {

    void txoptimizations$recordNewBounds();

    boolean txoptimizations$intersects(int left, int top, int right, int bottom);
}
