package com.teslicek.txoptimizations;

import com.teslicek.txoptimizations.mixin.LeashDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;

public final class LeashHolder {

    private static final int NO_DELAYED_HOLDER = 0;

    private LeashHolder() {
    }

    public static Entity of(Leashable leashable) {
        Leashable.LeashData leashData = leashable.getLeashData();

        if (leashData == null)
            return null;

        int delayedHolderId = ((LeashDataAccessor) (Object) leashData).txoptimizations$delayedLeashHolderId();

        if (delayedHolderId == NO_DELAYED_HOLDER)
            return leashData.leashHolder;

        Entity entity = (Entity) leashable;
        Entity holder = entity.level().getEntity(delayedHolderId);

        if (entity.level().isClientSide() && holder != null)
            leashData.setLeashHolder(holder);

        return leashData.leashHolder;
    }
}
