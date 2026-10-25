package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ReusedEntityState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityReusedStateMixin implements ReusedEntityState {

    @Unique
    private EntityRenderer<?, ?> txoptimizations$stateRenderer;

    @Unique
    private EntityRenderState txoptimizations$state;

    @Override
    public EntityRenderState txoptimizations$reusedState(EntityRenderer<?, ?> renderer) {
        return this.txoptimizations$stateRenderer == renderer ? this.txoptimizations$state : null;
    }

    @Override
    public void txoptimizations$keepState(EntityRenderer<?, ?> renderer, EntityRenderState state) {
        this.txoptimizations$stateRenderer = renderer;
        this.txoptimizations$state         = state;
    }
}
