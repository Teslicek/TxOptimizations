package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.PackedGizmoLines;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererGizmoReuseMixin {

    @Unique
    private DrawableGizmoPrimitives txoptimizations$standard;

    @Unique
    private DrawableGizmoPrimitives txoptimizations$alwaysOnTop;

    @Redirect(method = "finalizeGizmoCollection", at = @At(value = "NEW", target = "()Lnet/minecraft/client/renderer/gizmos/DrawableGizmoPrimitives;", ordinal = 0))
    private DrawableGizmoPrimitives txoptimizations$reuseStandard() {
        if (this.txoptimizations$standard == null)
            this.txoptimizations$standard = new DrawableGizmoPrimitives();
        else
            ((PackedGizmoLines) this.txoptimizations$standard).txoptimizations$clear();

        return this.txoptimizations$standard;
    }

    @Redirect(method = "finalizeGizmoCollection", at = @At(value = "NEW", target = "()Lnet/minecraft/client/renderer/gizmos/DrawableGizmoPrimitives;", ordinal = 1))
    private DrawableGizmoPrimitives txoptimizations$reuseAlwaysOnTop() {
        if (this.txoptimizations$alwaysOnTop == null)
            this.txoptimizations$alwaysOnTop = new DrawableGizmoPrimitives();
        else
            ((PackedGizmoLines) this.txoptimizations$alwaysOnTop).txoptimizations$clear();

        return this.txoptimizations$alwaysOnTop;
    }
}
