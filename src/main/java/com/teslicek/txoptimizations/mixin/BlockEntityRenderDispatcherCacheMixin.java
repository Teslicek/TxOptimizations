package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.RendererCache;
import java.util.Map;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherCacheMixin {

    @Redirect(
        method = "getRenderer(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"
        )
    )
    private Object txoptimizations$cachedRenderer(Map<?, ?> renderers, Object blockEntityType, BlockEntity blockEntity) {
        RendererCache cache = (RendererCache) blockEntity;

        if (cache.txoptimizations$getRendererSource() == renderers)
            return cache.txoptimizations$getRenderer();

        Object renderer = renderers.get(blockEntityType);
        cache.txoptimizations$setRenderer(renderers, renderer);

        return renderer;
    }
}
