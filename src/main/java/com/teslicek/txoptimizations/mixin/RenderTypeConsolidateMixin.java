package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderType.class)
public abstract class RenderTypeConsolidateMixin {

    @Unique
    private boolean txoptimizations$consolidates;

    @Shadow
    public abstract PrimitiveTopology primitiveTopology();

    @Inject(method = "<init>(Ljava/lang/String;Lnet/minecraft/client/renderer/rendertype/RenderSetup;)V", at = @At("RETURN"))
    private void txoptimizations$keepConsolidation(String name, RenderSetup state, CallbackInfo ci) {
        this.txoptimizations$consolidates = !this.primitiveTopology().connectedPrimitives;
    }

    @Overwrite
    public boolean canConsolidateConsecutiveGeometry() {
        return this.txoptimizations$consolidates;
    }
}
