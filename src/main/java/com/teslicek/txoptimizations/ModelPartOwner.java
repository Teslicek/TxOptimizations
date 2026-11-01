package com.teslicek.txoptimizations;

import org.jspecify.annotations.Nullable;

public interface ModelPartOwner {

    Object SHARED = new Object();

    @Nullable Object txoptimizations$owner();

    void txoptimizations$claim(Object model);
}
