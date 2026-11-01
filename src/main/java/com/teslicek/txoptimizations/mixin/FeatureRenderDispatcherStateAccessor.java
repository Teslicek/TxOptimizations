package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.GameRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FeatureRenderDispatcher.class)
public interface FeatureRenderDispatcherStateAccessor {

    @Accessor("gameRenderState")
    @Mutable
    void txoptimizations$setGameRenderState(GameRenderState value);
}
