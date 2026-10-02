package com.teslicek.txoptimizations.cull;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.caffeinemc.mods.sodium.client.util.collections.BitArray;

public final class OccluderBoxes {

    public static final int SIZE = 16;

    private static final int BLOCKS = SIZE * SIZE * SIZE;
    private static final int BITS   = 4;
    private static final int MASK   = SIZE - 1;

    private OccluderBoxes() {
    }

    public static int[] build(BitArray solid) {
        boolean[]    used  = new boolean[BLOCKS];
        IntArrayList boxes = new IntArrayList();

        for (int y = 0; y < SIZE; y ++) {
            for (int z = 0; z < SIZE; z ++) {
                for (int x = 0; x < SIZE; x ++) {
                    if (!isFree(solid, used, x, y, z))
                        continue;

                    int maxX = x;
                    int maxZ = z;
                    int maxY = y;

                    while (maxX + 1 < SIZE && isFree(solid, used, maxX + 1, y, z))
                        maxX ++;

                    while (maxZ + 1 < SIZE && isFreeRow(solid, used, x, maxX, y, maxZ + 1))
                        maxZ ++;

                    while (maxY + 1 < SIZE && isFreeSlab(solid, used, x, maxX, maxY + 1, z, maxZ))
                        maxY ++;

                    markUsed(used, x, maxX, y, maxY, z, maxZ);
                    boxes.add(pack(x, y, z, maxX, maxY, maxZ));
                }
            }
        }

        return boxes.isEmpty() ? null : boxes.toIntArray();
    }

    public static int minX(int box) {
        return box & MASK;
    }

    public static int minY(int box) {
        return box >>> BITS & MASK;
    }

    public static int minZ(int box) {
        return box >>> 2 * BITS & MASK;
    }

    public static int maxX(int box) {
        return box >>> 3 * BITS & MASK;
    }

    public static int maxY(int box) {
        return box >>> 4 * BITS & MASK;
    }

    public static int maxZ(int box) {
        return box >>> 5 * BITS & MASK;
    }

    private static int pack(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return minX | minY << BITS | minZ << 2 * BITS | maxX << 3 * BITS | maxY << 4 * BITS | maxZ << 5 * BITS;
    }

    private static int index(int x, int y, int z) {
        return x | z << BITS | y << 2 * BITS;
    }

    private static boolean isFree(BitArray solid, boolean[] used, int x, int y, int z) {
        int index = index(x, y, z);

        return solid.get(index) && !used[index];
    }

    private static boolean isFreeRow(BitArray solid, boolean[] used, int minX, int maxX, int y, int z) {
        for (int x = minX; x <= maxX; x ++) {
            if (!isFree(solid, used, x, y, z))
                return false;
        }

        return true;
    }

    private static boolean isFreeSlab(BitArray solid, boolean[] used, int minX, int maxX, int y, int minZ, int maxZ) {
        for (int z = minZ; z <= maxZ; z ++) {
            if (!isFreeRow(solid, used, minX, maxX, y, z))
                return false;
        }

        return true;
    }

    private static void markUsed(boolean[] used, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        for (int y = minY; y <= maxY; y ++) {
            for (int z = minZ; z <= maxZ; z ++) {
                for (int x = minX; x <= maxX; x ++)
                    used[index(x, y, z)] = true;
            }
        }
    }
}
