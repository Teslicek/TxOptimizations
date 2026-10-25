package com.teslicek.txoptimizations;

public interface PackedGizmoPrimitives {

    void txoptimizations$addLine(double startX, double startY, double startZ, double endX, double endY, double endZ, int color, float width);

    void txoptimizations$addBox(double x0, double y0, double z0, double x1, double y1, double z1, int color, int xColor, int yColor, int zColor, float width);

    void txoptimizations$addPoint(double x, double y, double z, int color, float size);

    void txoptimizations$clear();
}
