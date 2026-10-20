package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.GizmoShapes;
import net.minecraft.gizmos.ArrowGizmo;
import net.minecraft.gizmos.GizmoPrimitives;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ArrowGizmo.class, priority = 999)
public abstract class ArrowGizmoPackedMixin {

    @Shadow
    @Final
    private Vec3 start;

    @Shadow
    @Final
    private Vec3 end;

    @Shadow
    @Final
    private int color;

    @Shadow
    @Final
    private float width;

    @Overwrite
    public void emit(GizmoPrimitives primitives, float alphaMultiplier) {
        GizmoShapes.emitArrow(primitives, this.start.x, this.start.y, this.start.z, this.end.x, this.end.y, this.end.z, this.color, this.width, alphaMultiplier);
    }
}
