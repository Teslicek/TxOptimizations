package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.CompactVectorArray;
import com.mojang.blaze3d.vertex.VertexSorting;
import org.joml.Vector3f;

public abstract class PackedQuadSorting implements VertexSorting {

    public static final VertexSorting DISTANCE_TO_ORIGIN = byDistance(0.0F, 0.0F, 0.0F);
    public static final VertexSorting ORTHOGRAPHIC_Z     = new NegatedZ();

    private static final int PACKED_MINIMUM  = 16;
    private static final int INSERTION_LIMIT = 24;

    public static VertexSorting byDistance(float x, float y, float z) {
        return new Distance(new Vector3f(x, y, z));
    }

    protected abstract float key(CompactVectorArray points, int quad);

    @Override
    public final int[] sort(CompactVectorArray points) {
        int count = points.size();

        if (count < PACKED_MINIMUM)
            return this.sortFew(points, count);

        long[] packed = new long[count];

        for (int quad = 0; quad < count; quad ++)
            packed[quad] = (long) descendingKey(this.key(points, quad)) << Integer.SIZE | quad;

        sort(packed, 0, count, 2 * (Integer.SIZE - 1 - Integer.numberOfLeadingZeros(count)));

        int[] order = new int[count];

        for (int index = 0; index < count; index ++)
            order[index] = (int) packed[index];

        return order;
    }

    private int[] sortFew(CompactVectorArray points, int count) {
        int[] keys  = new int[count];
        int[] order = new int[count];

        for (int quad = 0; quad < count; quad ++) {
            int key   = descendingKey(this.key(points, quad));
            int index = quad - 1;

            keys[quad] = key;

            while (index >= 0 && keys[order[index]] > key) {
                order[index + 1] = order[index];
                index --;
            }

            order[index + 1] = quad;
        }

        return order;
    }

    private static int descendingKey(float key) {
        int bits = Float.floatToIntBits(key);

        return ~(bits ^ (bits >> 31 & Integer.MAX_VALUE));
    }

    private static void sort(long[] values, int from, int to, int depth) {
        while (to - from > INSERTION_LIMIT) {
            if (depth == 0) {
                heapSort(values, from, to);
                return;
            }

            depth --;

            int split = partition(values, from, to);

            if (split - from < to - split) {
                sort(values, from, split, depth);
                from = split;
            } else {
                sort(values, split, to, depth);
                to = split;
            }
        }

        insertionSort(values, from, to);
    }

    private static int partition(long[] values, int from, int to) {
        long pivot = median(values[from], values[from + to >>> 1], values[to - 1]);
        int  low   = from - 1;
        int  high  = to;

        while (true) {
            do {
                low ++;
            } while (values[low] < pivot);

            do {
                high --;
            } while (values[high] > pivot);

            if (low >= high)
                return high + 1;

            long value = values[low];

            values[low]  = values[high];
            values[high] = value;
        }
    }

    private static long median(long first, long second, long third) {
        if (first < second) {
            if (second < third)
                return second;

            return first < third ? third : first;
        }

        if (first < third)
            return first;

        return second < third ? third : second;
    }

    private static void insertionSort(long[] values, int from, int to) {
        for (int next = from + 1; next < to; next ++) {
            long value = values[next];
            int  index = next - 1;

            while (index >= from && values[index] > value) {
                values[index + 1] = values[index];
                index --;
            }

            values[index + 1] = value;
        }
    }

    private static void heapSort(long[] values, int from, int to) {
        int size = to - from;

        for (int root = size / 2 - 1; root >= 0; root --)
            siftDown(values, from, root, size);

        for (int end = size - 1; end > 0; end --) {
            long value = values[from];

            values[from]       = values[from + end];
            values[from + end] = value;

            siftDown(values, from, 0, end);
        }
    }

    private static void siftDown(long[] values, int from, int root, int size) {
        long value = values[from + root];
        int  child = 2 * root + 1;

        while (child < size) {
            if (child + 1 < size && values[from + child + 1] > values[from + child])
                child ++;

            if (values[from + child] <= value)
                break;

            values[from + root] = values[from + child];
            root                = child;
            child               = 2 * root + 1;
        }

        values[from + root] = value;
    }

    private static final class Distance extends PackedQuadSorting {

        private final Vector3f origin;

        private Distance(Vector3f origin) {
            this.origin = origin;
        }

        @Override
        protected float key(CompactVectorArray points, int quad) {
            return this.origin.distanceSquared(points.getX(quad), points.getY(quad), points.getZ(quad));
        }
    }

    private static final class NegatedZ extends PackedQuadSorting {

        @Override
        protected float key(CompactVectorArray points, int quad) {
            return -points.getZ(quad);
        }
    }
}
