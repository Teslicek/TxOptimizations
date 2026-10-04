package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.PackedGizmoLines;
import net.minecraft.gizmos.CuboidGizmo;
import net.minecraft.gizmos.GizmoPrimitives;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CuboidGizmo.class)
public abstract class CuboidGizmoSharedCornersMixin {

    @Shadow
    @Final
    private AABB aabb;

    @Shadow
    @Final
    private GizmoStyle style;

    @Shadow
    @Final
    private boolean coloredCornerStroke;

    @Overwrite
    public void emit(GizmoPrimitives primitives, float alphaMultiplier) {
        double x0 = this.aabb.minX;
        double y0 = this.aabb.minY;
        double z0 = this.aabb.minZ;
        double x1 = this.aabb.maxX;
        double y1 = this.aabb.maxY;
        double z1 = this.aabb.maxZ;

        if (primitives instanceof PackedGizmoLines packed && !this.style.hasFill()) {
            if (!this.style.hasStroke())
                return;

            int   color = this.style.multipliedStroke(alphaMultiplier);
            float width = this.style.strokeWidth();

            packed.txoptimizations$addLine(x0, y0, z0, x1, y0, z0, this.coloredCornerStroke ? ARGB.multiply(color, -34953) : color, width);
            packed.txoptimizations$addLine(x0, y0, z0, x0, y1, z0, this.coloredCornerStroke ? ARGB.multiply(color, -8913033) : color, width);
            packed.txoptimizations$addLine(x0, y0, z0, x0, y0, z1, this.coloredCornerStroke ? ARGB.multiply(color, -8947713) : color, width);
            packed.txoptimizations$addLine(x1, y0, z0, x1, y1, z0, color, width);
            packed.txoptimizations$addLine(x1, y1, z0, x0, y1, z0, color, width);
            packed.txoptimizations$addLine(x0, y1, z0, x0, y1, z1, color, width);
            packed.txoptimizations$addLine(x0, y1, z1, x0, y0, z1, color, width);
            packed.txoptimizations$addLine(x0, y0, z1, x1, y0, z1, color, width);
            packed.txoptimizations$addLine(x1, y0, z1, x1, y0, z0, color, width);
            packed.txoptimizations$addLine(x0, y1, z1, x1, y1, z1, color, width);
            packed.txoptimizations$addLine(x1, y0, z1, x1, y1, z1, color, width);
            packed.txoptimizations$addLine(x1, y1, z0, x1, y1, z1, color, width);

            return;
        }

        Vec3 a = new Vec3(x0, y0, z0);
        Vec3 b = new Vec3(x1, y0, z0);
        Vec3 c = new Vec3(x1, y1, z0);
        Vec3 d = new Vec3(x0, y1, z0);
        Vec3 e = new Vec3(x0, y0, z1);
        Vec3 f = new Vec3(x1, y0, z1);
        Vec3 g = new Vec3(x1, y1, z1);
        Vec3 h = new Vec3(x0, y1, z1);

        if (this.style.hasFill()) {
            int color = this.style.multipliedFill(alphaMultiplier);

            primitives.addQuad(b, c, g, f, color);
            primitives.addQuad(a, e, h, d, color);
            primitives.addQuad(a, d, c, b, color);
            primitives.addQuad(e, f, g, h, color);
            primitives.addQuad(d, h, g, c, color);
            primitives.addQuad(a, b, f, e, color);
        }

        if (!this.style.hasStroke())
            return;

        int   color = this.style.multipliedStroke(alphaMultiplier);
        float width = this.style.strokeWidth();

        primitives.addLine(a, b, this.coloredCornerStroke ? ARGB.multiply(color, -34953) : color, width);
        primitives.addLine(a, d, this.coloredCornerStroke ? ARGB.multiply(color, -8913033) : color, width);
        primitives.addLine(a, e, this.coloredCornerStroke ? ARGB.multiply(color, -8947713) : color, width);
        primitives.addLine(b, c, color, width);
        primitives.addLine(c, d, color, width);
        primitives.addLine(d, h, color, width);
        primitives.addLine(h, e, color, width);
        primitives.addLine(e, f, color, width);
        primitives.addLine(f, b, color, width);
        primitives.addLine(h, g, color, width);
        primitives.addLine(f, g, color, width);
        primitives.addLine(c, g, color, width);
    }
}
