package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.GizmoLineList;
import com.teslicek.txoptimizations.GizmoPointList;
import java.util.List;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrawableGizmoPrimitives.Group.class)
public abstract class DrawableGizmoGroupPackedMixin {

    @Shadow
    @Final
    @Mutable
    private List<DrawableGizmoPrimitives.Line> lines;

    @Shadow
    @Final
    @Mutable
    private List<DrawableGizmoPrimitives.Point> points;

    @Inject(method = "<init>(Z)V", at = @At("RETURN"))
    private void txoptimizations$packLines(boolean opaque, CallbackInfo ci) {
        if (!this.lines.isEmpty() || !this.points.isEmpty())
            throw new IllegalStateException("Gizmo group lines or points were filled during construction");

        this.lines  = new GizmoLineList();
        this.points = new GizmoPointList();
    }
}
