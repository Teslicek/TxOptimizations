package com.teslicek.txoptimizations;

import org.jspecify.annotations.Nullable;

public interface TextCullingSlot {

    @Nullable TextCullingBox txoptimizations$cullingBox();

    void txoptimizations$setCullingBox(TextCullingBox box);
}
