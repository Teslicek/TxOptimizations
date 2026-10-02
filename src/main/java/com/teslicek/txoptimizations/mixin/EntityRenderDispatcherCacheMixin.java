package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.RendererCache;
import java.util.Map;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.PlayerModelType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherCacheMixin {

    @Shadow
    private Map<EntityType<?>, EntityRenderer<?, ?>> renderers;

    @Shadow
    private Map<PlayerModelType, AvatarRenderer<AbstractClientPlayer>> playerRenderers;

    @Shadow
    private Map<PlayerModelType, AvatarRenderer<ClientMannequin>> mannequinRenderers;

    @SuppressWarnings("rawtypes")
    @Shadow
    private AvatarRenderer getAvatarRenderer(Map renderers, Avatar entity) {
        throw new AssertionError();
    }

    @SuppressWarnings("unchecked")
    @Overwrite
    public <T extends Entity> EntityRenderer<? super T, ?> getRenderer(T entity) {
        RendererCache cache = (RendererCache) entity;

        if (cache.txoptimizations$getRendererSource() == this.renderers)
            return (EntityRenderer<? super T, ?>) cache.txoptimizations$getRenderer();

        if (entity instanceof AbstractClientPlayer player)
            return (EntityRenderer<? super T, ?>) this.getAvatarRenderer(this.playerRenderers, player);

        if (entity instanceof ClientMannequin mannequin)
            return (EntityRenderer<? super T, ?>) this.getAvatarRenderer(this.mannequinRenderers, mannequin);

        EntityRenderer<?, ?> renderer = this.renderers.get(entity.getType());

        cache.txoptimizations$setRenderer(this.renderers, renderer);

        return (EntityRenderer<? super T, ?>) renderer;
    }
}
