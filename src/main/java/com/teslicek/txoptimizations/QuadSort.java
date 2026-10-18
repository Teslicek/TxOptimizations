package com.teslicek.txoptimizations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class QuadSort {

    private static final int RADIX_THRESHOLD = 80;
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

    public static int[] sorted(int[] keys, int length) {
        long[] values = pack(keys, length, new long[length]);

        if (length > RADIX_THRESHOLD)
            values = radixSort(values, new long[length], length, new int[BUCKETS]);
        else
            Arrays.sort(values, 0, length);

        int[] result = new int[length];

        for (int index = 0; index < length; index ++)
            result[index] = (int) values[index];

        return result;
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

        long[] values = pack(keys, length, packed);

        if (length > RADIX_THRESHOLD)
            values = radixSort(values, scratch, length, COUNTS);
        else
            Arrays.sort(values, 0, length);

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

    private static long[] pack(int[] keys, int length, long[] values) {
        for (int index = 0; index < length; index ++)
            values[index] = (long) keys[index] << KEY_SHIFT | index;

        return values;
    }

    private static long[] radixSort(long[] values, long[] other, int length, int[] counts) {
        for (int digit = 0; digit < DIGITS; digit ++) {
            int shift = KEY_SHIFT + digit * DIGIT_BITS;
            int flip  = digit == SIGN_DIGIT ? SIGN_BUCKET : 0;

            Arrays.fill(counts, 0);

            for (int index = 0; index < length; index ++)
                counts[bucket(values[index], shift, flip)] ++;

            if (counts[bucket(values[0], shift, flip)] == length)
                continue;

            int sum = 0;

            for (int bucket = 0; bucket < BUCKETS; bucket ++) {
                int count = counts[bucket];

                counts[bucket]  = sum;
                sum            += count;
            }

            for (int index = 0; index < length; index ++) {
                long value = values[index];

                other[counts[bucket(value, shift, flip)] ++] = value;
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
