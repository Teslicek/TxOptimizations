package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.PackedGizmoPrimitives;
import com.teslicek.txoptimizations.RenderStateBuffers;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererGizmoReuseMixin {

    @Unique
    private final DrawableGizmoPrimitives[] txoptimizations$standard = new DrawableGizmoPrimitives[2];

    @Unique
    private final DrawableGizmoPrimitives[] txoptimizations$alwaysOnTop = new DrawableGizmoPrimitives[2];

    @Redirect(method = "finalizeGizmoCollection", at = @At(value = "NEW", target = "()Lnet/minecraft/client/renderer/gizmos/DrawableGizmoPrimitives;", ordinal = 0))
    private DrawableGizmoPrimitives txoptimizations$reuseStandard() {
        return txoptimizations$reuse(this.txoptimizations$standard);
    }

    @Redirect(method = "finalizeGizmoCollection", at = @At(value = "NEW", target = "()Lnet/minecraft/client/renderer/gizmos/DrawableGizmoPrimitives;", ordinal = 1))
    private DrawableGizmoPrimitives txoptimizations$reuseAlwaysOnTop() {
        return txoptimizations$reuse(this.txoptimizations$alwaysOnTop);
    }

    @Unique
    private static DrawableGizmoPrimitives txoptimizations$reuse(DrawableGizmoPrimitives[] perFrame) {
        int parity = RenderStateBuffers.parity();

        if (perFrame[parity] == null)
            perFrame[parity] = new DrawableGizmoPrimitives();
        else
            ((PackedGizmoPrimitives) perFrame[parity]).txoptimizations$clear();

        return perFrame[parity];
    }
}
