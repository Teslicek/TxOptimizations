package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.RendererCache;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherCacheMixin {

    @Shadow
    private Map<EntityType<?>, EntityRenderer<?, ?>> renderers;

    @Inject(method = "getRenderer(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/client/renderer/entity/EntityRenderer;", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$reuseRenderer(Entity entity, CallbackInfoReturnable<EntityRenderer<?, ?>> cir) {
        RendererCache cache = (RendererCache) entity;

        if (cache.txoptimizations$getRendererSource() == this.renderers)
            cir.setReturnValue((EntityRenderer<?, ?>) cache.txoptimizations$getRenderer());
    }

    @Redirect(
        method = "getRenderer(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/client/renderer/entity/EntityRenderer;",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"
        )
    )
    private Object txoptimizations$storeRenderer(Map<?, ?> renderers, Object entityType, Entity entity) {
        Object renderer = renderers.get(entityType);
        ((RendererCache) entity).txoptimizations$setRenderer(renderers, renderer);

        return renderer;
    }
}
