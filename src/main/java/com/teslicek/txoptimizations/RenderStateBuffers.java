package com.teslicek.txoptimizations;

import com.teslicek.txoptimizations.mixin.FeatureRenderDispatcherStateAccessor;
import com.teslicek.txoptimizations.mixin.GameRendererStateAccessor;
import com.teslicek.txoptimizations.mixin.GuiRendererStateAccessor;
import com.teslicek.txoptimizations.mixin.GuiStateAccessor;
import com.teslicek.txoptimizations.mixin.LevelExtractorStateAccessor;
import com.teslicek.txoptimizations.mixin.LevelRendererStateAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import org.jspecify.annotations.Nullable;

public final class RenderStateBuffers {

    private static GameRenderState @Nullable [] trees;
    private static int                          current;

    private RenderStateBuffers() {
    }

    public static int parity() {
        return current;
    }

    public static void swap(Minecraft minecraft) {
        GameRenderer renderer = minecraft.gameRenderer;

        if (trees == null)
            trees = new GameRenderState[] {renderer.gameRenderState(), new GameRenderState()};

        if (renderer.gameRenderState() != trees[current])
            throw new IllegalStateException("The game render state was replaced outside the render state buffers");

        current ^= 1;

        GameRenderState           next     = trees[current];
        GameRendererStateAccessor accessor = (GameRendererStateAccessor) renderer;

        accessor.txoptimizations$setGameRenderState(next);
        ((FeatureRenderDispatcherStateAccessor) renderer.featureRenderDispatcher()).txoptimizations$setGameRenderState(next);
        ((LevelExtractorStateAccessor) minecraft.levelExtractor).txoptimizations$setLevelRenderState(next.levelRenderState);
        ((LevelRendererStateAccessor) minecraft.levelRenderer).txoptimizations$setLevelRenderState(next.levelRenderState);
        ((LevelRendererStateAccessor) minecraft.levelRenderer).txoptimizations$setOptionsRenderState(next.optionsRenderState);
        ((GuiStateAccessor) minecraft.gui).txoptimizations$setGuiRenderState(next.guiRenderState);
        ((GuiRendererStateAccessor) accessor.txoptimizations$guiRenderer()).txoptimizations$setRenderState(next.guiRenderState);
    }
}
