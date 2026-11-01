package com.teslicek.txoptimizations.mixin;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.PassengerIndex;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.List;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class EntityPassengerIndexMixin implements PassengerIndex {

    @Unique
    private static final int LINEAR_PASSENGERS = 8;

    @Shadow
    private ImmutableList<Entity> passengers;

    @Unique
    private ImmutableList<Entity> txoptimizations$indexedPassengers;

    @Unique
    private Int2IntOpenHashMap txoptimizations$passengerIndices;

    @Redirect(method = "getDefaultPassengerAttachmentPoint", at = @At(value = "INVOKE", target = "Ljava/util/List;indexOf(Ljava/lang/Object;)I"), require = 1)
    private static int txoptimizations$indexedPassenger(List<Entity> passengers, Object passenger, @Local(argsOnly = true, ordinal = 0) Entity vehicle) {
        if (passengers.size() <= LINEAR_PASSENGERS)
            return passengers.indexOf(passenger);

        return ((PassengerIndex) vehicle).txoptimizations$passengerIndex((Entity) passenger);
    }

    @Override
    public int txoptimizations$passengerIndex(Entity passenger) {
        if (this.txoptimizations$indexedPassengers != this.passengers) {
            Int2IntOpenHashMap indices = new Int2IntOpenHashMap(this.passengers.size());

            indices.defaultReturnValue(-1);

            for (int index = 0; index < this.passengers.size(); index ++)
                indices.putIfAbsent(this.passengers.get(index).getId(), index);

            this.txoptimizations$passengerIndices  = indices;
            this.txoptimizations$indexedPassengers = this.passengers;
        }

        return this.txoptimizations$passengerIndices.get(passenger.getId());
    }
}
