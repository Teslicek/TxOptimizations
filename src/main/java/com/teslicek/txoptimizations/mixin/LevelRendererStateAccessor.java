package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelRenderer.class)
public interface LevelRendererStateAccessor {

    @Accessor("levelRenderState")
    @Mutable
    void txoptimizations$setLevelRenderState(LevelRenderState value);

    @Accessor("optionsRenderState")
    @Mutable
    void txoptimizations$setOptionsRenderState(OptionsRenderState value);

    @Invoker("repositionCamera")
    void txoptimizations$repositionCamera(CameraRenderState camera);
}
