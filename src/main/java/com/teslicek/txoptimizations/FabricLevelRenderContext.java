package com.teslicek.txoptimizations;

import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.level.LevelRenderState;

public interface FabricLevelRenderContext {

    void txoptimizations$prepareBeforeFirstRender(LevelRenderState levelRenderState, SubmitNodeStorage storage);
}
