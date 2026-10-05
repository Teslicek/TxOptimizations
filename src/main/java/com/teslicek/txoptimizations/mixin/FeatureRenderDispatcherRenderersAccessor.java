package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRendererMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FeatureRenderDispatcher.class)
public interface FeatureRenderDispatcherRenderersAccessor {

    @Accessor("featureRenderers")
    FeatureRendererMap txoptimizations$featureRenderers();
}
