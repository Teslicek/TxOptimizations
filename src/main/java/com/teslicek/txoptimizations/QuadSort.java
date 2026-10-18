package com.teslicek.txoptimizations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class QuadSort {

    public static final int FEW_QUADS = 16;

    private static final int RADIX_THRESHOLD = 80;
    private static final int INSERTION_LIMIT = 24;
    private static final int DIGITS          = 4;
    private static final int DIGIT_BITS      = 8;
    private static final int BUCKETS         = 1 << DIGIT_BITS;
    private static final int KEY_SHIFT       = Integer.SIZE;
    private static final int SIGN_DIGIT      = DIGITS - 1;
    private static final int SIGN_BUCKET     = BUCKETS >> 1;

    private static final List<int[]> PREVIOUS = new ArrayList<>();
    private static final int[]       COUNTS   = new int[BUCKETS];

    private static Thread owner;
    private static long   frame = -1L;
    private static int    call;
    private static int[]  keys    = new int[0];
    private static long[] packed  = new long[0];
    private static long[] scratch = new long[0];

    private QuadSort() {
    }

    public static int key(float distance) {
        int bits = Float.floatToIntBits(distance);

        return ~(bits ^ bits >> 31 & Integer.MAX_VALUE);
    }

    public static long pack(int key, int quad) {
        return (long) key << KEY_SHIFT | quad;
    }

    public static int[] stableOrder(int[] keys) {
        int[] order = new int[keys.length];

        for (int quad = 0; quad < keys.length; quad ++) {
            int key   = keys[quad];
            int index = quad - 1;

            while (index >= 0 && keys[order[index]] > key) {
                order[index + 1] = order[index];
                index --;
            }

            order[index + 1] = quad;
        }

        return order;
    }

    public static int[] order(long[] values) {
        sort(values, 0, values.length);

        int[] order = new int[values.length];

        for (int index = 0; index < values.length; index ++)
            order[index] = (int) values[index];

        return order;
    }

    public static int[] keys(int length) {
        checkOwner();

        if (keys.length < length)
            keys = new int[length];

        return keys;
    }

    public static int[] sort(int[] keys, int length) {
        checkOwner();

        int   slot     = nextSlot();
        int[] previous = slot < PREVIOUS.size() ? PREVIOUS.get(slot) : null;

        if (previous != null && previous.length == length && isSortedOrder(previous, keys))
            return previous;

        if (packed.length < length) {
            packed  = new long[length];
            scratch = new long[length];
        }

        long[] values = packed;

        for (int index = 0; index < length; index ++)
            values[index] = pack(keys[index], index);

        if (length > RADIX_THRESHOLD)
            values = radixSort(values, scratch, length);
        else
            sort(values, 0, length);

        int[] result = previous != null && previous.length == length ? previous : new int[length];

        for (int index = 0; index < length; index ++)
            result[index] = (int) values[index];

        if (slot < PREVIOUS.size())
            PREVIOUS.set(slot, result);
        else
            PREVIOUS.add(result);

        return result;
    }

    private static boolean isSortedOrder(int[] order, int[] keys) {
        for (int position = 1; position < order.length; position ++) {
            int before     = order[position - 1];
            int after      = order[position];
            int comparison = Integer.compare(keys[before], keys[after]);

            if (comparison > 0 || comparison == 0 && before > after)
                return false;
        }

        return true;
    }

    private static long[] radixSort(long[] values, long[] other, int length) {
        for (int digit = 0; digit < DIGITS; digit ++) {
            int shift = KEY_SHIFT + digit * DIGIT_BITS;
            int flip  = digit == SIGN_DIGIT ? SIGN_BUCKET : 0;

            Arrays.fill(COUNTS, 0);

            for (int index = 0; index < length; index ++)
                COUNTS[bucket(values[index], shift, flip)] ++;

            if (COUNTS[bucket(values[0], shift, flip)] == length)
                continue;

            int sum = 0;

            for (int bucket = 0; bucket < BUCKETS; bucket ++) {
                int count = COUNTS[bucket];

                COUNTS[bucket]  = sum;
                sum            += count;
            }

            for (int index = 0; index < length; index ++) {
                long value = values[index];

                other[COUNTS[bucket(value, shift, flip)] ++] = value;
            }

            long[] swap = values;

            values = other;
            other  = swap;
        }

        return values;
    }

    private static int bucket(long value, int shift, int flip) {
        return ((int) (value >>> shift) & BUCKETS - 1) ^ flip;
    }

    private static void sort(long[] values, int from, int to) {
        sort(values, from, to, 2 * (Integer.SIZE - 1 - Integer.numberOfLeadingZeros(to - from)));
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

    private static int nextSlot() {
        long current = ClientClock.frame();

        if (current != frame) {
            frame = current;
            call  = 0;
        }

        return call ++;
    }

    private static void checkOwner() {
        Thread thread = Thread.currentThread();

        if (owner == null)
            owner = thread;
        else if (owner != thread)
            throw new IllegalStateException("Quad sorting is only supported on one thread, called from " + thread.getName());
    }
}
