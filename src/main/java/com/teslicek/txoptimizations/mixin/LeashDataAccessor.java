package com.teslicek.txoptimizations.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.world.entity.Leashable$LeashData")
public interface LeashDataAccessor {

    @Accessor("delayedLeashHolderId")
    int txoptimizations$delayedLeashHolderId();
}
