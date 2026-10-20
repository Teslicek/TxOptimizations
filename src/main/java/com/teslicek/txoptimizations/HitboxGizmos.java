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
    private static final int   VALUES      = 6;
    private static final int   INITIAL     = 64;
    private static final int   POOL_SIZE   = 4;
    private static final float ARROW_WIDTH = 2.5F;

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
    private volatile boolean               emitted;

    private HitboxGizmos() {
    }

    @Override
    public void emit(GizmoPrimitives primitives, float alphaMultiplier) {
        if (this.emitted)
            throw new IllegalStateException("Hitbox gizmos were emitted twice");

        for (int index = 0; index < this.count; index ++) {
            int base = index * VALUES;

            switch (this.kinds[index]) {
                case CUBOID -> GizmoShapes.emitCuboid(primitives, this.values[base], this.values[base + 1], this.values[base + 2], this.values[base + 3], this.values[base + 4], this.values[base + 5], (GizmoStyle) this.objects[index], false, alphaMultiplier);
                case POINT -> primitives.addPoint((Vec3) this.objects[index], ARGB.multiplyAlpha(this.colors[index], alphaMultiplier), this.sizes[index]);
                case ARROW -> GizmoShapes.emitArrow(primitives, this.values[base], this.values[base + 1], this.values[base + 2], this.values[base + 3], this.values[base + 4], this.values[base + 5], this.colors[index], this.sizes[index], alphaMultiplier);
                default -> throw new IllegalStateException("Unknown hitbox gizmo kind " + this.kinds[index]);
            }
        }

        Arrays.fill(this.objects, 0, this.count, null);
        this.emitted = true;
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

    public static void cuboid(double x1, double y1, double z1, double x2, double y2, double z2, GizmoStyle style) {
        HitboxGizmos batch = batch();

        if (batch == null) {
            Gizmos.cuboid(new AABB(x1, y1, z1, x2, y2, z2), style);
            return;
        }

        int index = batch.reserve(CUBOID);

        batch.putValues(index, Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
        batch.objects[index] = style;
    }

    public static void point(Vec3 position, int color, float size) {
        HitboxGizmos batch = batch();

        if (batch == null) {
            Gizmos.point(position, color, size);
            return;
        }

        int index = batch.reserve(POINT);

        batch.objects[index] = position;
        batch.colors[index]  = color;
        batch.sizes[index]   = size;
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
