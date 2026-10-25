package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.EntityStateReuse;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererStateReuseMixin {

    @Redirect(method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;createRenderState()Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"))
    private EntityRenderState txoptimizations$reuseWorldState(EntityRenderer<?, ?> renderer, Entity entity, float partialTicks) {
        return EntityStateReuse.stateFor(renderer, entity);
    }
}
