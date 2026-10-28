package com.teslicek.txoptimizations;

public interface FrustumBoxTest {

    int OUTSIDE = 0;
    int MIXED   = 1;
    int INSIDE  = 2;

    boolean txoptimizations$isBoxVisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);

    void txoptimizations$capturePlanes(double[] planes);
}
