package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.GizmoLineList;
import com.teslicek.txoptimizations.GizmoPointList;
import com.teslicek.txoptimizations.PackedGizmoPrimitives;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = DrawableGizmoPrimitives.class, priority = 999)
public abstract class DrawableGizmoPrimitivesPackedMixin implements PackedGizmoPrimitives {

    @Shadow
    private boolean isEmpty;

    @Shadow
    @Final
    private DrawableGizmoPrimitives.Group opaque;

    @Shadow
    @Final
    private DrawableGizmoPrimitives.Group translucent;

    @Shadow
    protected abstract DrawableGizmoPrimitives.Group getGroup(int color);

    @Overwrite
    public void addLine(Vec3 start, Vec3 end, int color, float width) {
        this.txoptimizations$addLine(start.x, start.y, start.z, end.x, end.y, end.z, color, width);
    }

    @Overwrite
    public void addPoint(Vec3 pos, int color, float size) {
        this.txoptimizations$addPoint(pos.x, pos.y, pos.z, color, size);
    }

    @Override
    public void txoptimizations$addPoint(double x, double y, double z, int color, float size) {
        ((GizmoPointList) this.getGroup(color).points()).add(x, y, z, color, size);
        this.isEmpty = false;
    }

    @Override
    public void txoptimizations$addLine(double startX, double startY, double startZ, double endX, double endY, double endZ, int color, float width) {
        ((GizmoLineList) this.getGroup(color).lines()).add(startX, startY, startZ, endX, endY, endZ, color, width);
        this.isEmpty = false;
    }

    @Override
    public void txoptimizations$addBox(double x0, double y0, double z0, double x1, double y1, double z1, int color, int xColor, int yColor, int zColor, float width) {
        DrawableGizmoPrimitives.Group group = this.getGroup(color);

        if (this.getGroup(xColor) != group || this.getGroup(yColor) != group || this.getGroup(zColor) != group)
            throw new IllegalStateException("Box edge colors " + Integer.toHexString(xColor) + ", " + Integer.toHexString(yColor) + " and " + Integer.toHexString(zColor) + " do not share the transparency of " + Integer.toHexString(color));

        ((GizmoLineList) group.lines()).addBox(x0, y0, z0, x1, y1, z1, color, xColor, yColor, zColor, width);
        this.isEmpty = false;
    }

    @Override
    public void txoptimizations$clear() {
        txoptimizations$clear(this.opaque);
        txoptimizations$clear(this.translucent);
        this.isEmpty = true;
    }

    @Unique
    private static void txoptimizations$clear(DrawableGizmoPrimitives.Group group) {
        group.lines().clear();
        group.quads().clear();
        group.triangleFans().clear();
        group.texts().clear();
        group.points().clear();
    }
}
