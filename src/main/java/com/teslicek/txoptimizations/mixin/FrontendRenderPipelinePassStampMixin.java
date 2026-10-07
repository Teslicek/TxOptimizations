package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.frontend.FrontendRenderPipeline;
import com.teslicek.txoptimizations.PipelinePassStamp;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FrontendRenderPipeline.class)
public abstract class FrontendRenderPipelinePassStampMixin implements PipelinePassStamp {

    @Unique
    private long txoptimizations$validatedPass;

    @Override
    public long txoptimizations$getValidatedPass() {
        return this.txoptimizations$validatedPass;
    }

    @Override
    public void txoptimizations$setValidatedPass(long pass) {
        this.txoptimizations$validatedPass = pass;
    }
}
