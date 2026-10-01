package com.teslicek.txoptimizations;

import java.util.Arrays;
import java.util.Collection;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;

public final class NeighborTable {

    private static final int MAX_BITS = 30;

    private final Property<?>[] properties;
    private final Object[]      states;
    private final int[]         shifts;
    private final int[]         masks;

    private NeighborTable(Property<?>[] properties, Object[] states, int[] shifts, int[] masks) {
        this.properties = properties;
        this.states     = states;
        this.shifts     = shifts;
        this.masks      = masks;
    }

    public Object neighbor(int index, int propertyIndex, int valueIndex) {
        int    shift    = this.shifts[propertyIndex];
        Object neighbor = this.states[index & ~(this.masks[propertyIndex] << shift) | valueIndex << shift];

        if (neighbor == null)
            throw new IllegalStateException("No neighbor state for value " + valueIndex + " of property " + propertyIndex + " from index " + index + " with properties " + Arrays.toString(this.properties));

        return neighbor;
    }

    public static void assign(Collection<? extends StateHolder<?, ?>> states) {
        if (states.isEmpty())
            throw new IllegalArgumentException("Cannot build a neighbor table without states");

        Property<?>[] properties = states.iterator().next().propertyKeys;
        int[]         shifts     = new int[properties.length];
        int[]         masks      = new int[properties.length];
        int           bits       = 0;

        for (int i = 0; i < properties.length; i ++) {
            int width = Integer.SIZE - Integer.numberOfLeadingZeros(properties[i].getPossibleValues().size() - 1);
            shifts[i] = bits;
            masks[i]  = (1 << width) - 1;
            bits     += width;
        }

        if (bits > MAX_BITS)
            throw new IllegalStateException("Neighbor table needs " + bits + " bits for properties " + Arrays.toString(properties));

        Object[]      table         = new Object[1 << bits];
        NeighborTable neighborTable = new NeighborTable(properties, table, shifts, masks);

        for (StateHolder<?, ?> state : states) {
            if (state.propertyKeys != properties)
                throw new IllegalStateException("State " + state + " does not share the property keys of its definition");

            int index = 0;

            for (int i = 0; i < properties.length; i ++)
                index |= internalIndex(properties[i], state.propertyValues[i]) << shifts[i];

            if (table[index] != null)
                throw new IllegalStateException("States " + table[index] + " and " + state + " map to the same neighbor index " + index);

            table[index] = state;
            ((NeighborTableHolder) state).txoptimizations$setNeighborTable(neighborTable, index);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> int internalIndex(Property<T> property, Comparable<?> value) {
        int index = property.getInternalIndex((T) value);

        if (index < 0)
            throw new IllegalStateException("Value " + value + " is not allowed for property " + property);

        return index;
    }
}
