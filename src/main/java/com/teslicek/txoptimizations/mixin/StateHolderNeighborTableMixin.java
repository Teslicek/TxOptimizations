package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.NeighborTable;
import com.teslicek.txoptimizations.NeighborTableHolder;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = StateHolder.class, priority = 900)
public abstract class StateHolderNeighborTableMixin<O, S> implements NeighborTableHolder {

    @Shadow
    @Final
    protected O owner;

    @Unique
    private NeighborTable txoptimizations$neighborTable;

    @Unique
    private int txoptimizations$neighborIndex;

    @Overwrite
    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>, V extends T> S setValueInternal(Property<T> property, int propertyIndex, V value) {
        int valueIndex = property.getInternalIndex(value);

        if (valueIndex < 0)
            throw new IllegalArgumentException("Cannot set property " + property + " to " + value + " on " + this.owner + ", it is not an allowed value");

        return (S) this.txoptimizations$neighborTable.neighbor(this.txoptimizations$neighborIndex, propertyIndex, valueIndex);
    }

    @Override
    public void txoptimizations$setNeighborTable(NeighborTable neighborTable, int index) {
        this.txoptimizations$neighborTable = neighborTable;
        this.txoptimizations$neighborIndex = index;
    }
}
