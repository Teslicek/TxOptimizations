package com.teslicek.txoptimizations;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3fc;

public final class ItemBoundsCache {

    private static final int MAX_ENTRIES = 4096;

    private static final Map<Key, AABB> BOUNDS = new HashMap<>();
    private static final Key            PROBE  = new Key();

    private ItemBoundsCache() {
    }

    public static AABB find(Supplier<Vector3fc[]> extents, ItemTransform itemTransform, boolean leftHand, Matrix4f localTransform) {
        PROBE.set(extents, itemTransform, leftHand, localTransform);

        return BOUNDS.get(PROBE);
    }

    public static AABB store(AABB bounds, Supplier<Vector3fc[]> extents, ItemTransform itemTransform, boolean leftHand, Matrix4f localTransform) {
        if (BOUNDS.size() >= MAX_ENTRIES)
            BOUNDS.clear();

        Key key = new Key();

        key.set(extents, itemTransform, leftHand, localTransform);
        BOUNDS.put(key, bounds);

        return bounds;
    }

    private static final class Key {

        private final float[]          transform = new float[16];
        private Supplier<Vector3fc[]>  extents;
        private ItemTransform          itemTransform;
        private boolean                leftHand;
        private int                    hash;

        private void set(Supplier<Vector3fc[]> extents, ItemTransform itemTransform, boolean leftHand, Matrix4f localTransform) {
            this.extents       = extents;
            this.itemTransform = itemTransform;
            this.leftHand      = leftHand;
            localTransform.get(this.transform);

            int value = System.identityHashCode(extents);

            value = 31 * value + System.identityHashCode(itemTransform);
            value = 31 * value + Boolean.hashCode(leftHand);

            for (float element : this.transform)
                value = 31 * value + Float.floatToRawIntBits(element);

            this.hash = value;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Key that) || this.extents != that.extents || this.itemTransform != that.itemTransform || this.leftHand != that.leftHand)
                return false;

            for (int i = 0; i < this.transform.length; i ++) {
                if (Float.floatToRawIntBits(this.transform[i]) != Float.floatToRawIntBits(that.transform[i]))
                    return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return this.hash;
        }
    }
}
