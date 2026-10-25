package com.teslicek.txoptimizations;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public interface ReusedEntityState {

    EntityRenderState txoptimizations$reusedState(EntityRenderer<?, ?> renderer);

    void txoptimizations$keepState(EntityRenderer<?, ?> renderer, EntityRenderState state);
}
