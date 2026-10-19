package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.backend.opengl.GlSurface;
import com.teslicek.txoptimizations.AcquiredImageDiscard;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GlSurface.class)
public abstract class GlSurfaceDiscardMixin implements AcquiredImageDiscard {

    @Override
    public void txoptimizations$discardAcquired() {
    }
}
