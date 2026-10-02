package com.teslicek.txoptimizations.mixin.cull;

import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Warden.class)
public interface WardenHeartbeatAccessor {

    @Invoker("getHeartBeatDelay")
    int txoptimizations$getHeartBeatDelay();
}
