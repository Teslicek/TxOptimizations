package com.teslicek.txoptimizations;

import net.minecraft.resources.Identifier;

public interface MapAtlasRenderState {

    Identifier txoptimizations$getAtlasTexture();

    float txoptimizations$getAtlasU();

    float txoptimizations$getAtlasV();

    void txoptimizations$setAtlas(Identifier atlasTexture, float u, float v);
}
