package com.teslicek.txoptimizations;

public interface MapAtlasSource {

    int NO_LOCATION = -1;

    MapAtlas txoptimizations$getMapAtlas(int location);

    int txoptimizations$getMapAtlasLocation(int mapId);

    int txoptimizations$allocateMapAtlasLocation(int mapId);
}
