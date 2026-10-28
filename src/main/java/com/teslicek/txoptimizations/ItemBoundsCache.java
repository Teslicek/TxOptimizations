package com.teslicek.txoptimizations;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class ItemBoundsCache {

    private static final int MAX_ENTRIES = 4096;

    private static final Map<Entry, Entry> BOUNDS = new HashMap<>();
    private static final Entry             PROBE  = new Entry();

    private ItemBoundsCache() {
    }

    public static @Nullable Entry find(Supplier<Vector3fc[]> extents, ItemTransform itemTransform, boolean leftHand, Matrix4fc localTransform) {
        PROBE.set(extents, itemTransform, leftHand, localTransform);

        return BOUNDS.get(PROBE);
    }

    public static Entry store(AABB bounds, Supplier<Vector3fc[]> extents, ItemTransform itemTransform, boolean leftHand, Matrix4fc localTransform) {
        if (BOUNDS.size() >= MAX_ENTRIES)
            BOUNDS.clear();

        Entry entry = new Entry();

        entry.set(extents, itemTransform, leftHand, localTransform);
        entry.bounds = bounds;
        BOUNDS.put(entry, entry);

        return entry;
    }

    public static final class Entry {

        private final float[]         transform = new float[16];
        private Supplier<Vector3fc[]> extents;
        private ItemTransform         itemTransform;
        private boolean               leftHand;
        private int                   hash;
        private AABB                  bounds;

        private Entry() {
        }

        public AABB bounds() {
            return this.bounds;
        }

        public boolean matches(Supplier<Vector3fc[]> extents, ItemTransform itemTransform, boolean leftHand, Matrix4fc localTransform) {
            float[] transform = this.transform;

            return this.extents == extents && this.itemTransform == itemTransform && this.leftHand == leftHand
                && same(transform[0], localTransform.m00()) && same(transform[1], localTransform.m01()) && same(transform[2], localTransform.m02()) && same(transform[3], localTransform.m03())
                && same(transform[4], localTransform.m10()) && same(transform[5], localTransform.m11()) && same(transform[6], localTransform.m12()) && same(transform[7], localTransform.m13())
                && same(transform[8], localTransform.m20()) && same(transform[9], localTransform.m21()) && same(transform[10], localTransform.m22()) && same(transform[11], localTransform.m23())
                && same(transform[12], localTransform.m30()) && same(transform[13], localTransform.m31()) && same(transform[14], localTransform.m32()) && same(transform[15], localTransform.m33());
        }

        private void set(Supplier<Vector3fc[]> extents, ItemTransform itemTransform, boolean leftHand, Matrix4fc localTransform) {
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
            if (!(other instanceof Entry that) || this.extents != that.extents || this.itemTransform != that.itemTransform || this.leftHand != that.leftHand)
                return false;

            for (int i = 0; i < this.transform.length; i ++) {
                if (!same(this.transform[i], that.transform[i]))
                    return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return this.hash;
        }

        private static boolean same(float first, float second) {
            return Float.floatToRawIntBits(first) == Float.floatToRawIntBits(second);
        }
    }
}
