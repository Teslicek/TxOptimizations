package com.teslicek.txoptimizations;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.RandomAccess;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.world.phys.Vec3;

public final class GizmoLineList extends AbstractList<DrawableGizmoPrimitives.Line> implements RandomAccess {

    private static final int COORDINATES = 6;
    private static final int INITIAL     = 64;
    private static final int BOX_LINES   = 12;

    private double[] coordinates = new double[INITIAL * COORDINATES];
    private int[]    colors      = new int[INITIAL];
    private float[]  widths      = new float[INITIAL];
    private int      size;

    public void add(double startX, double startY, double startZ, double endX, double endY, double endZ, int color, float width) {
        this.ensureCapacity(1);
        this.put(this.size, startX, startY, startZ, endX, endY, endZ, color, width);
        this.size ++;
        this.modCount ++;
    }

    public void addBox(double x0, double y0, double z0, double x1, double y1, double z1, int color, int xColor, int yColor, int zColor, float width) {
        this.ensureCapacity(BOX_LINES);

        int line = this.size;

        this.put(line, x0, y0, z0, x1, y0, z0, xColor, width);
        this.put(line + 1, x0, y0, z0, x0, y1, z0, yColor, width);
        this.put(line + 2, x0, y0, z0, x0, y0, z1, zColor, width);
        this.put(line + 3, x1, y0, z0, x1, y1, z0, color, width);
        this.put(line + 4, x1, y1, z0, x0, y1, z0, color, width);
        this.put(line + 5, x0, y1, z0, x0, y1, z1, color, width);
        this.put(line + 6, x0, y1, z1, x0, y0, z1, color, width);
        this.put(line + 7, x0, y0, z1, x1, y0, z1, color, width);
        this.put(line + 8, x1, y0, z1, x1, y0, z0, color, width);
        this.put(line + 9, x0, y1, z1, x1, y1, z1, color, width);
        this.put(line + 10, x1, y0, z1, x1, y1, z1, color, width);
        this.put(line + 11, x1, y1, z0, x1, y1, z1, color, width);
        this.size += BOX_LINES;
        this.modCount ++;
    }

    private void ensureCapacity(int lines) {
        int capacity = this.colors.length;

        while (this.size + lines > capacity)
            capacity *= 2;

        if (capacity == this.colors.length)
            return;

        this.coordinates = Arrays.copyOf(this.coordinates, capacity * COORDINATES);
        this.colors      = Arrays.copyOf(this.colors, capacity);
        this.widths      = Arrays.copyOf(this.widths, capacity);
    }

    private void put(int line, double startX, double startY, double startZ, double endX, double endY, double endZ, int color, float width) {
        int base = line * COORDINATES;

        this.coordinates[base]     = startX;
        this.coordinates[base + 1] = startY;
        this.coordinates[base + 2] = startZ;
        this.coordinates[base + 3] = endX;
        this.coordinates[base + 4] = endY;
        this.coordinates[base + 5] = endZ;
        this.colors[line]          = color;
        this.widths[line]          = width;
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
