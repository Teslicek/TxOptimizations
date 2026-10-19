package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.backend.api.GpuSurfaceBackend;
import com.mojang.renderpearl.frontend.FrontendGpuSurface;
import com.teslicek.txoptimizations.AcquiredImageDiscard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FrontendGpuSurface.class)
public abstract class FrontendGpuSurfaceDiscardMixin implements AcquiredImageDiscard {

    @Shadow
    @Final
    private GpuSurfaceBackend backend;

    @Shadow
    private boolean hasImageAcquired;

    @Shadow
    private boolean hasBlittedTexture;

    @Override
    public void txoptimizations$discardAcquired() {
        if (!this.hasImageAcquired || this.hasBlittedTexture)
            throw new IllegalStateException("Only an acquired image that nothing was copied to can be discarded");

        ((AcquiredImageDiscard) this.backend).txoptimizations$discardAcquired();
        this.hasImageAcquired = false;
    }
}
