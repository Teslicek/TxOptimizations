package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.GizmoShapes;
import net.minecraft.gizmos.CuboidGizmo;
import net.minecraft.gizmos.GizmoPrimitives;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CuboidGizmo.class, priority = 999)
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
        GizmoShapes.emitCuboid(primitives, this.aabb.minX, this.aabb.minY, this.aabb.minZ, this.aabb.maxX, this.aabb.maxY, this.aabb.maxZ, this.style, this.coloredCornerStroke, alphaMultiplier);
    }
}
