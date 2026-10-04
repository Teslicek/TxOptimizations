package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.GizmoLineList;
import com.teslicek.txoptimizations.PackedGizmoLines;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DrawableGizmoPrimitives.class)
public abstract class DrawableGizmoPrimitivesPackedMixin implements PackedGizmoLines {

    @Shadow
    private boolean isEmpty;

    @Shadow
    protected abstract DrawableGizmoPrimitives.Group getGroup(int color);

    @Overwrite
    public void addLine(Vec3 start, Vec3 end, int color, float width) {
        this.txoptimizations$addLine(start.x, start.y, start.z, end.x, end.y, end.z, color, width);
    }

    @Override
    public void txoptimizations$addLine(double startX, double startY, double startZ, double endX, double endY, double endZ, int color, float width) {
        ((GizmoLineList) this.getGroup(color).lines()).add(startX, startY, startZ, endX, endY, endZ, color, width);
        this.isEmpty = false;
    }
}
