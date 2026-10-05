package com.teslicek.txoptimizations;

import net.minecraft.world.phys.AABB;

public interface CullBoxCache {

    AABB txoptimizations$inflateCullBox(AABB base, double amount);
}
