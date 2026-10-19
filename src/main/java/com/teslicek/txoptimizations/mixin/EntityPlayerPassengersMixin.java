package com.teslicek.txoptimizations.mixin;

import com.google.common.collect.ImmutableList;
import java.util.stream.Stream;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Entity.class, priority = 999)
public abstract class EntityPlayerPassengersMixin {

    @Shadow
    private ImmutableList<Entity> passengers;

    @Shadow
    private Stream<Entity> getIndirectPassengersStream() {
        throw new AssertionError();
    }

    @Overwrite
    public int countPlayerPassengers() {
        if (this.passengers.isEmpty())
            return 0;

        return (int) this.getIndirectPassengersStream().filter(passenger -> passenger instanceof Player).count();
    }
}
