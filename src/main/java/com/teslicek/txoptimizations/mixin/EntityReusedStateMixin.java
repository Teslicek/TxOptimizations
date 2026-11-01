package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.RenderStateBuffers;
import com.teslicek.txoptimizations.ReusedEntityState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityReusedStateMixin implements ReusedEntityState {

    @Unique
    private EntityRenderer<?, ?> txoptimizations$evenRenderer;

    @Unique
    private EntityRenderState txoptimizations$evenState;

    @Unique
    private EntityRenderer<?, ?> txoptimizations$oddRenderer;

    @Unique
    private EntityRenderState txoptimizations$oddState;

    @Override
    public EntityRenderState txoptimizations$reusedState(EntityRenderer<?, ?> renderer) {
        if (RenderStateBuffers.parity() == 0)
            return this.txoptimizations$evenRenderer == renderer ? this.txoptimizations$evenState : null;

        return this.txoptimizations$oddRenderer == renderer ? this.txoptimizations$oddState : null;
    }

    @Override
    public void txoptimizations$keepState(EntityRenderer<?, ?> renderer, EntityRenderState state) {
        if (RenderStateBuffers.parity() == 0) {
            this.txoptimizations$evenRenderer = renderer;
            this.txoptimizations$evenState    = state;

            return;
        }

        this.txoptimizations$oddRenderer = renderer;
        this.txoptimizations$oddState    = state;
    }
}
