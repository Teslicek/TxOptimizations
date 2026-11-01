package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface GameRendererStateAccessor {

    @Accessor("gameRenderState")
    @Mutable
    void txoptimizations$setGameRenderState(GameRenderState value);

    @Accessor("gameRenderState")
    GameRenderState txoptimizations$gameRenderState();

    @Accessor("guiRenderer")
    GuiRenderer txoptimizations$guiRenderer();
}
