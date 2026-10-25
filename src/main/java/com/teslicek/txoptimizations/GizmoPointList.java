package com.teslicek.txoptimizations;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.RandomAccess;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.world.phys.Vec3;

public final class GizmoPointList extends AbstractList<DrawableGizmoPrimitives.Point> implements RandomAccess {

    private static final int AXES    = 3;
    private static final int INITIAL = 64;

    private double[] positions = new double[INITIAL * AXES];
    private int[]    colors    = new int[INITIAL];
    private float[]  sizes     = new float[INITIAL];
    private int      size;

    public void add(double x, double y, double z, int color, float pointSize) {
        int index = this.size;

        if (index == this.colors.length) {
            int capacity = index * 2;

            this.positions = Arrays.copyOf(this.positions, capacity * AXES);
            this.colors    = Arrays.copyOf(this.colors, capacity);
            this.sizes     = Arrays.copyOf(this.sizes, capacity);
        }

        this.positions[index * AXES]     = x;
        this.positions[index * AXES + 1] = y;
        this.positions[index * AXES + 2] = z;
        this.colors[index]               = color;
        this.sizes[index]                = pointSize;
        this.size                        = index + 1;
        this.modCount ++;
    }

    public double coordinate(int index, int axis) {
        return this.positions[index * AXES + axis];
    }

    public int color(int index) {
        return this.colors[index];
    }

    public float pointSize(int index) {
        return this.sizes[index];
    }

    @Override
    public boolean add(DrawableGizmoPrimitives.Point point) {
        this.add(point.pos().x(), point.pos().y(), point.pos().z(), point.color(), point.size());

        return true;
    }

    @Override
    public DrawableGizmoPrimitives.Point get(int index) {
        if (index < 0 || index >= this.size)
            throw new IndexOutOfBoundsException("Point " + index + " of " + this.size);

        return new DrawableGizmoPrimitives.Point(new Vec3(this.coordinate(index, 0), this.coordinate(index, 1), this.coordinate(index, 2)), this.colors[index], this.sizes[index]);
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
}
