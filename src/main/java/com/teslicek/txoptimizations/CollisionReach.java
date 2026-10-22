package com.teslicek.txoptimizations;

public interface CollisionReach {

    boolean txoptimizations$matches(Object data, int paletteSize, int reads);

    boolean txoptimizations$reaches();

    void txoptimizations$remember(Object data, int paletteSize, int reads, boolean reaches);
}
