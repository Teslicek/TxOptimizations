package com.teslicek.txoptimizations;

import net.minecraft.core.SectionPos;

public record SectionGridWindow(int minX, int maxX, int minZ, int maxZ, int firstGridX, int firstGridZ, int size) {

    public boolean contains(int sectionX, int sectionZ) {
        return sectionX >= this.minX && sectionX <= this.maxX && sectionZ >= this.minZ && sectionZ <= this.maxZ;
    }

    public int gridX(int sectionX) {
        return this.wrap(this.firstGridX + (sectionX - this.minX));
    }

    public int gridZ(int sectionZ) {
        return this.wrap(this.firstGridZ + (sectionZ - this.minZ));
    }

    private int wrap(int grid) {
        return grid >= this.size ? grid - this.size : grid;
    }

    public static SectionGridWindow around(SectionPos center, int radius, int size) {
        int minX = center.x() - radius;
        int minZ = center.z() - radius;

        return new SectionGridWindow(minX, center.x() + radius, minZ, center.z() + radius, Math.floorMod(minX, size), Math.floorMod(minZ, size), size);
    }
}
