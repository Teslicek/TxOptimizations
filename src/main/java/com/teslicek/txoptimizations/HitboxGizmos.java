package com.teslicek.txoptimizations;

import com.teslicek.txoptimizations.mixin.GizmosAccessor;
import java.util.Arrays;
import java.util.List;
import net.minecraft.gizmos.Gizmo;
import net.minecraft.gizmos.GizmoPrimitives;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.SimpleGizmoCollector;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class HitboxGizmos implements Gizmo {

    private static final int   CUBOID      = 0;
    private static final int   POINT       = 1;
    private static final int   ARROW       = 2;
    private static final int   GROUP       = 3;
    private static final int   VALUES      = 6;
    private static final int   INITIAL     = 64;
    private static final int   POOL_SIZE   = 4;
    private static final float ARROW_WIDTH = 2.5F;
    private static final float ARROW_HEAD  = 1.5F;

    private static final HitboxGizmos[] POOL = new HitboxGizmos[POOL_SIZE];

    private static @Nullable HitboxGizmos open;
    private static int                    replaced;

    private int[]                          kinds   = new int[INITIAL];
    private double[]                       values  = new double[INITIAL * VALUES];
    private Object[]                       objects = new Object[INITIAL];
    private int[]                          colors  = new int[INITIAL];
    private float[]                        sizes   = new float[INITIAL];
    private int                            count;
    private @Nullable SimpleGizmoCollector collector;
    private @Nullable GizmoProperties      instance;
    private int                            position;
    private int                            group   = -1;
    private volatile boolean               emitted;

    private HitboxGizmos() {
    }

    @Override
    public void emit(GizmoPrimitives primitives, float alphaMultiplier) {
        if (this.emitted)
            throw new IllegalStateException("Hitbox gizmos were emitted twice");

        this.closeGroup();

        for (int index = 0; index < this.count; index ++) {
            int base = index * VALUES;

            switch (this.kinds[index]) {
                case GROUP -> {
                    if (DisplayCulling.isGizmoHidden(this.values[base], this.values[base + 1], this.values[base + 2], this.values[base + 3], this.values[base + 4], this.values[base + 5]))
                        index += this.colors[index];
                }
                case CUBOID -> GizmoShapes.emitCuboid(primitives, this.values[base], this.values[base + 1], this.values[base + 2], this.values[base + 3], this.values[base + 4], this.values[base + 5], (GizmoStyle) this.objects[index], false, alphaMultiplier);
                case POINT -> GizmoShapes.emitPoint(primitives, this.values[base], this.values[base + 1], this.values[base + 2], ARGB.multiplyAlpha(this.colors[index], alphaMultiplier), this.sizes[index]);
                case ARROW -> GizmoShapes.emitArrow(primitives, this.values[base], this.values[base + 1], this.values[base + 2], this.values[base + 3], this.values[base + 4], this.values[base + 5], this.colors[index], this.sizes[index], alphaMultiplier);
                default -> throw new IllegalStateException("Unknown hitbox gizmo kind " + this.kinds[index]);
            }
        }

        Arrays.fill(this.objects, 0, this.count, null);
        this.emitted = true;
    }

    private void closeGroup() {
        if (this.group < 0)
            return;

        this.colors[this.group] = this.count - this.group - 1;
        this.group              = -1;
    }

    private void include(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (this.group < 0)
            return;

        int base = this.group * VALUES;

        this.values[base]     = Math.min(this.values[base], minX);
        this.values[base + 1] = Math.min(this.values[base + 1], minY);
        this.values[base + 2] = Math.min(this.values[base + 2], minZ);
        this.values[base + 3] = Math.max(this.values[base + 3], maxX);
        this.values[base + 4] = Math.max(this.values[base + 4], maxY);
        this.values[base + 5] = Math.max(this.values[base + 5], maxZ);
    }

    private boolean isLast(SimpleGizmoCollector current) {
        List<SimpleGizmoCollector.GizmoInstance> gizmos = current.getGizmos();

        return current == this.collector && gizmos.size() == this.position && gizmos.get(this.position - 1) == this.instance;
    }

    private int reserve(int kind) {
        int index = this.count;

        if (index == this.kinds.length) {
            int capacity = index * 2;

            this.kinds   = Arrays.copyOf(this.kinds, capacity);
            this.values  = Arrays.copyOf(this.values, capacity * VALUES);
            this.objects = Arrays.copyOf(this.objects, capacity);
            this.colors  = Arrays.copyOf(this.colors, capacity);
            this.sizes   = Arrays.copyOf(this.sizes, capacity);
        }

        this.kinds[index] = kind;
        this.count        = index + 1;

        return index;
    }

    private void putValues(int index, double first, double second, double third, double fourth, double fifth, double sixth) {
        int base = index * VALUES;

        this.values[base]     = first;
        this.values[base + 1] = second;
        this.values[base + 2] = third;
        this.values[base + 3] = fourth;
        this.values[base + 4] = fifth;
        this.values[base + 5] = sixth;
    }

    public static void beginEntity() {
        HitboxGizmos batch = batch();

        if (batch == null)
            return;

        batch.closeGroup();

        int index = batch.reserve(GROUP);

        batch.putValues(index, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        batch.group = index;
    }

    public static void endEntity() {
        if (!RenderThreadStack.isOwner())
            return;

        HitboxGizmos batch = open;

        if (batch != null)
            batch.closeGroup();
    }

    public static void cuboid(double x1, double y1, double z1, double x2, double y2, double z2, GizmoStyle style) {
        HitboxGizmos batch = batch();

        if (batch == null) {
            Gizmos.cuboid(new AABB(x1, y1, z1, x2, y2, z2), style);
            return;
        }

        int index = batch.reserve(CUBOID);

        double minX = Math.min(x1, x2);
        double minY = Math.min(y1, y2);
        double minZ = Math.min(z1, z2);
        double maxX = Math.max(x1, x2);
        double maxY = Math.max(y1, y2);
        double maxZ = Math.max(z1, z2);

        batch.putValues(index, minX, minY, minZ, maxX, maxY, maxZ);
        batch.objects[index] = style;
        batch.include(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static void point(double x, double y, double z, int color, float size) {
        HitboxGizmos batch = batch();

        if (batch == null) {
            Gizmos.point(new Vec3(x, y, z), color, size);
            return;
        }

        int index = batch.reserve(POINT);
        int base  = index * VALUES;

        batch.values[base]     = x;
        batch.values[base + 1] = y;
        batch.values[base + 2] = z;
        batch.colors[index]    = color;
        batch.sizes[index]     = size;
        batch.include(x, y, z, x, y, z);
    }

    public static void arrow(double startX, double startY, double startZ, double endX, double endY, double endZ, int color) {
        HitboxGizmos batch = batch();

        if (batch == null) {
            Gizmos.arrow(new Vec3(startX, startY, startZ), new Vec3(endX, endY, endZ), color);
            return;
        }

        int index = batch.reserve(ARROW);

        batch.putValues(index, startX, startY, startZ, endX, endY, endZ);
        batch.colors[index] = color;
        batch.sizes[index]  = ARROW_WIDTH;
        batch.include(Math.min(startX, endX) - ARROW_HEAD, Math.min(startY, endY) - ARROW_HEAD, Math.min(startZ, endZ) - ARROW_HEAD, Math.max(startX, endX) + ARROW_HEAD, Math.max(startY, endY) + ARROW_HEAD, Math.max(startZ, endZ) + ARROW_HEAD);
    }

    private static @Nullable HitboxGizmos batch() {
        if (!RenderThreadStack.isOwner())
            return null;

        if (!(GizmoCollectorCache.get(GizmosAccessor.txoptimizations$collector()) instanceof SimpleGizmoCollector current) || current.getClass() != SimpleGizmoCollector.class)
            return null;

        HitboxGizmos last = open;

        if (last != null && last.isLast(current))
            return last;

        HitboxGizmos batch = obtain();

        batch.instance  = current.add(batch);
        batch.collector = current;
        batch.position  = current.getGizmos().size();
        open            = batch;

        return batch;
    }

    private static HitboxGizmos obtain() {
        for (HitboxGizmos pooled : POOL) {
            if (pooled != null && pooled.emitted) {
                pooled.count   = 0;
                pooled.emitted = false;

                return pooled;
            }
        }

        HitboxGizmos batch = new HitboxGizmos();

        POOL[replaced] = batch;
        replaced       = (replaced + 1) % POOL_SIZE;

        return batch;
    }
}
