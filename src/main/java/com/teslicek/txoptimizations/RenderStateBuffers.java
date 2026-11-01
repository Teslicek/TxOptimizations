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
    private static volatile GameRenderState     renderTree;

    private RenderStateBuffers() {
    }

    public static int parity() {
        return current;
    }

    public static void swap(Minecraft minecraft) {
        GameRenderer renderer = minecraft.gameRenderer;

        if (trees == null) {
            trees      = new GameRenderState[] {renderer.gameRenderState(), new GameRenderState()};
            renderTree = trees[0];
        }

        current ^= 1;

        GameRenderState next = trees[current];

        ((GameRendererStateAccessor) renderer).txoptimizations$setGameRenderState(next);
        ((LevelExtractorStateAccessor) minecraft.levelExtractor).txoptimizations$setLevelRenderState(next.levelRenderState);
        ((GuiStateAccessor) minecraft.gui).txoptimizations$setGuiRenderState(next.guiRenderState);
    }

    public static void handOff() {
        if (trees == null)
            throw new IllegalStateException("A render job was handed off before the first render state swap");

        renderTree = trees[current];
    }

    public static void bindRender(Minecraft minecraft) {
        GameRenderState            tree     = renderTree;
        GameRenderer               renderer = minecraft.gameRenderer;
        LevelRendererStateAccessor level    = (LevelRendererStateAccessor) minecraft.levelRenderer;

        ((FeatureRenderDispatcherStateAccessor) renderer.featureRenderDispatcher()).txoptimizations$setGameRenderState(tree);
        level.txoptimizations$setLevelRenderState(tree.levelRenderState);
        level.txoptimizations$setOptionsRenderState(tree.optionsRenderState);
        ((GuiRendererStateAccessor) ((GameRendererStateAccessor) renderer).txoptimizations$guiRenderer()).txoptimizations$setRenderState(tree.guiRenderState);
    }

    public static GameRenderState renderTree() {
        return renderTree;
    }

    public static GameRenderState forRole(GameRenderState extractionTree) {
        return RenderWorker.isCurrent() ? renderTree : extractionTree;
    }
}
