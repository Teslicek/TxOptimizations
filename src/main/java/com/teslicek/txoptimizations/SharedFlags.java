package com.teslicek.txoptimizations;

import net.minecraft.network.syncher.EntityDataAccessor;

public interface SharedFlags {

    void txoptimizations$storeSyncedValue(EntityDataAccessor<?> accessor, Object value);
}
