package com.teslicek.txoptimizations;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.RandomAccess;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.world.phys.Vec3;

public final class GizmoLineList extends AbstractList<DrawableGizmoPrimitives.Line> implements RandomAccess {

    public static final int BOX_LINES = 12;

    private static final int   COORDINATES = 6;
    private static final int   AXES        = 3;
    private static final int   INITIAL     = 64;
    private static final int[] BOX_FROM    = {0, 0, 0, 1, 3, 2, 6, 4, 5, 6, 5, 3};
    private static final int[] BOX_TO      = {1, 2, 4, 3, 2, 6, 4, 5, 1, 7, 7, 7};

    private double[]  coordinates = new double[INITIAL * COORDINATES];
    private int[]     colors      = new int[INITIAL];
    private int[]     axisColors  = new int[INITIAL * AXES];
    private float[]   widths      = new float[INITIAL];
    private boolean[] boxes       = new boolean[INITIAL];
    private int       entries;
    private int       size;

    public void add(double startX, double startY, double startZ, double endX, double endY, double endZ, int color, float width) {
        int entry = this.reserve();

        this.put(entry, startX, startY, startZ, endX, endY, endZ, color, width);
        this.boxes[entry] = false;
        this.size ++;
        this.modCount ++;
    }

    public void addBox(double x0, double y0, double z0, double x1, double y1, double z1, int color, int xColor, int yColor, int zColor, float width) {
        int entry = this.reserve();

        this.put(entry, x0, y0, z0, x1, y1, z1, color, width);
        this.axisColors[entry * AXES]     = xColor;
        this.axisColors[entry * AXES + 1] = yColor;
        this.axisColors[entry * AXES + 2] = zColor;
        this.boxes[entry]                 = true;
        this.size                        += BOX_LINES;
        this.modCount ++;
    }

    public int entryCount() {
        return this.entries;
    }

    public boolean isBox(int entry) {
        return this.boxes[entry];
    }

    public double coordinate(int entry, int component) {
        return this.coordinates[entry * COORDINATES + component];
    }

    public int color(int entry) {
        return this.colors[entry];
    }

    public int edgeColor(int entry, int edge) {
        return edge < AXES ? this.axisColors[entry * AXES + edge] : this.colors[entry];
    }

    public float width(int entry) {
        return this.widths[entry];
    }

    public static int edgeFrom(int edge) {
        return BOX_FROM[edge];
    }

    public static int edgeTo(int edge) {
        return BOX_TO[edge];
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

        int line = 0;

        for (int entry = 0; entry < this.entries; entry ++) {
            if (!this.boxes[entry]) {
                if (line == index)
                    return new DrawableGizmoPrimitives.Line(this.point(entry, 0), this.point(entry, 3), this.colors[entry], this.widths[entry]);

                line ++;
                continue;
            }

            if (index < line + BOX_LINES) {
                int edge = index - line;

                return new DrawableGizmoPrimitives.Line(this.corner(entry, BOX_FROM[edge]), this.corner(entry, BOX_TO[edge]), this.edgeColor(entry, edge), this.widths[entry]);
            }

            line += BOX_LINES;
        }

        throw new IllegalStateException("Line " + index + " is missing from " + this.entries + " entries holding " + this.size + " lines");
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public void clear() {
        this.entries = 0;
        this.size    = 0;
        this.modCount ++;
    }

    private Vec3 point(int entry, int first) {
        int base = entry * COORDINATES + first;

        return new Vec3(this.coordinates[base], this.coordinates[base + 1], this.coordinates[base + 2]);
    }

    private Vec3 corner(int entry, int corner) {
        int base = entry * COORDINATES;

        return new Vec3(this.coordinates[base + ((corner & 1) == 0 ? 0 : 3)], this.coordinates[base + ((corner & 2) == 0 ? 1 : 4)], this.coordinates[base + ((corner & 4) == 0 ? 2 : 5)]);
    }

    private int reserve() {
        int entry = this.entries;

        if (entry == this.colors.length) {
            int capacity = entry * 2;

            this.coordinates = Arrays.copyOf(this.coordinates, capacity * COORDINATES);
            this.colors      = Arrays.copyOf(this.colors, capacity);
            this.axisColors  = Arrays.copyOf(this.axisColors, capacity * AXES);
            this.widths      = Arrays.copyOf(this.widths, capacity);
            this.boxes       = Arrays.copyOf(this.boxes, capacity);
        }

        this.entries = entry + 1;

        return entry;
    }

    private void put(int entry, double startX, double startY, double startZ, double endX, double endY, double endZ, int color, float width) {
        int base = entry * COORDINATES;

        this.coordinates[base]     = startX;
        this.coordinates[base + 1] = startY;
        this.coordinates[base + 2] = startZ;
        this.coordinates[base + 3] = endX;
        this.coordinates[base + 4] = endY;
        this.coordinates[base + 5] = endZ;
        this.colors[entry]         = color;
        this.widths[entry]         = width;
    }
}
