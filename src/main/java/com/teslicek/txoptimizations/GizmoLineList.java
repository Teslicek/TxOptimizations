package com.teslicek.txoptimizations;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.RandomAccess;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.world.phys.Vec3;

public final class GizmoLineList extends AbstractList<DrawableGizmoPrimitives.Line> implements RandomAccess {

    private static final int COORDINATES = 6;
    private static final int INITIAL     = 64;

    private double[] coordinates = new double[INITIAL * COORDINATES];
    private int[]    colors      = new int[INITIAL];
    private float[]  widths      = new float[INITIAL];
    private int      size;

    public void add(double startX, double startY, double startZ, double endX, double endY, double endZ, int color, float width) {
        if (this.size == this.colors.length) {
            int capacity = this.size * 2;

            this.coordinates = Arrays.copyOf(this.coordinates, capacity * COORDINATES);
            this.colors      = Arrays.copyOf(this.colors, capacity);
            this.widths      = Arrays.copyOf(this.widths, capacity);
        }

        int base = this.size * COORDINATES;

        this.coordinates[base]     = startX;
        this.coordinates[base + 1] = startY;
        this.coordinates[base + 2] = startZ;
        this.coordinates[base + 3] = endX;
        this.coordinates[base + 4] = endY;
        this.coordinates[base + 5] = endZ;
        this.colors[this.size]     = color;
        this.widths[this.size]     = width;
        this.size ++;
        this.modCount ++;
    }

    @Override
    public boolean add(DrawableGizmoPrimitives.Line line) {
        this.add(line.start().x(), line.start().y(), line.start().z(), line.end().x(), line.end().y(), line.end().z(), line.color(), line.width());

        return true;
    }

    @Override
    public DrawableGizmoPrimitives.Line get(int index) {
        if (index < 0 || index >= this.size)
            throw new IndexOutOfBoundsException("Line " + index + " of " + this.size);

        int base = index * COORDINATES;

        return new DrawableGizmoPrimitives.Line(new Vec3(this.coordinates[base], this.coordinates[base + 1], this.coordinates[base + 2]), new Vec3(this.coordinates[base + 3], this.coordinates[base + 4], this.coordinates[base + 5]), this.colors[index], this.widths[index]);
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public void clear() {
        this.size = 0;
        this.modCount ++;
    }

    public double coordinate(int index, int component) {
        return this.coordinates[index * COORDINATES + component];
    }

    public int color(int index) {
        return this.colors[index];
    }

    public float width(int index) {
        return this.widths[index];
    }
}
