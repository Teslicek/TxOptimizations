package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.GizmoLineList;
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
public abstract class DrawableGizmoGroupLinesMixin {

    @Shadow
    @Final
    @Mutable
    private List<DrawableGizmoPrimitives.Line> lines;

    @Inject(method = "<init>(Z)V", at = @At("RETURN"))
    private void txoptimizations$packLines(boolean opaque, CallbackInfo ci) {
        if (!this.lines.isEmpty())
            throw new IllegalStateException("Gizmo group lines were filled during construction");

        this.lines = new GizmoLineList();
    }
}
