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

    public static int[] keys(int length) {
        checkOwner();

        if (keys.length < length)
            keys = new int[length];

        return keys;
    }

    public static int[] sort(int[] keys, int length) {
        checkOwner();

        boolean signed   = length <= RADIX_THRESHOLD;
        int     slot     = nextSlot();
        int[]   previous = slot < PREVIOUS.size() ? PREVIOUS.get(slot) : null;

        if (previous != null && previous.length == length && isStableOrder(previous, keys, signed))
            return previous;

        long[] values = signed ? sortSigned(keys, length) : sortUnsigned(keys, length);
        int[]  order  = previous != null && previous.length == length ? previous : new int[length];

        for (int index = 0; index < length; index ++)
            order[index] = (int) values[index];

        if (slot < PREVIOUS.size())
            PREVIOUS.set(slot, order);
        else
            PREVIOUS.add(order);

        return order;
    }

    private static boolean isStableOrder(int[] order, int[] keys, boolean signed) {
        for (int position = 1; position < order.length; position ++) {
            int before     = order[position - 1];
            int after      = order[position];
            int comparison = signed ? Integer.compare(keys[before], keys[after]) : Integer.compareUnsigned(keys[before], keys[after]);

            if (comparison > 0 || comparison == 0 && before > after)
                return false;
        }

        return true;
    }

    private static long[] sortSigned(int[] keys, int length) {
        long[] values = buffer(length);

        for (int index = 0; index < length; index ++)
            values[index] = (long) keys[index] << KEY_SHIFT | index;

        Arrays.sort(values, 0, length);

        return values;
    }

    private static long[] sortUnsigned(int[] keys, int length) {
        long[] values = buffer(length);
        long[] other  = scratch;

        for (int index = 0; index < length; index ++)
            values[index] = (long) keys[index] << KEY_SHIFT | index;

        for (int digit = 0; digit < DIGITS; digit ++) {
            int shift = KEY_SHIFT + digit * DIGIT_BITS;

            Arrays.fill(COUNTS, 0);

            for (int index = 0; index < length; index ++)
                COUNTS[(int) (values[index] >>> shift) & BUCKETS - 1] ++;

            if (COUNTS[(int) (values[0] >>> shift) & BUCKETS - 1] == length)
                continue;

            int sum = 0;

            for (int bucket = 0; bucket < BUCKETS; bucket ++) {
                int count = COUNTS[bucket];

                COUNTS[bucket]  = sum;
                sum            += count;
            }

            for (int index = 0; index < length; index ++) {
                long value = values[index];

                other[COUNTS[(int) (value >>> shift) & BUCKETS - 1] ++] = value;
            }

            long[] swap = values;

            values = other;
            other  = swap;
        }

        return values;
    }

    private static long[] buffer(int length) {
        if (packed.length < length) {
            packed  = new long[length];
            scratch = new long[length];
        }

        return packed;
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
