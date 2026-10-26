package com.teslicek.txoptimizations;

import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public interface ItemCullingBounds {

    @Nullable
    AABB txoptimizations$cullingBounds();
}
