package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererSodiumCullMixin<T extends Entity, S extends EntityRenderState> {

    @ModifyReturnValue(method = "shouldRender", at = @At("RETURN"))
    private boolean txoptimizations$cullHiddenSections(boolean visible, T entity, Frustum culler, double camX, double camY, double camZ, float partialTicks) {
        if (!visible)
            return false;

        SodiumWorldRenderer sodium = SodiumWorldRenderer.instanceNullable();

        return sodium == null || sodium.isEntityVisible((EntityRenderer<T, S>) (Object) this, entity, partialTicks);
    }
}
