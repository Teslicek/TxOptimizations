package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.BlockEntityDistance;
import com.teslicek.txoptimizations.ModelCopies;
import com.teslicek.txoptimizations.RendererCache;
import java.util.Map;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.BrushableBlockRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {

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

    @Redirect(method = "tryExtractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;shouldRender(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/phys/Vec3;)Z"))
    private boolean txoptimizations$distanceWithoutVector(BlockEntityRenderer<?, ?> renderer, BlockEntity blockEntity, Vec3 cameraPosition) {
        return BlockEntityDistance.shouldRender(renderer, blockEntity, cameraPosition);
    }

    @ModifyExpressionValue(method = "tryExtractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;getRenderer(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;"))
    private BlockEntityRenderer<?, ?> txoptimizations$skipUndustedBrushable(BlockEntityRenderer<?, ?> renderer, @Local(argsOnly = true) BlockEntity blockEntity) {
        if (!(renderer instanceof BrushableBlockRenderer))
            return renderer;

        BlockState state = blockEntity.getBlockState();

        if (state.hasProperty(BlockStateProperties.DUSTED) && state.getValue(BlockStateProperties.DUSTED) == 0)
            return null;

        return renderer;
    }

    @Shadow
    private Map<BlockEntityType<?>, BlockEntityRenderer<?, ?>> renderers;

    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    private void txoptimizations$copyModels(ResourceManager resourceManager, CallbackInfo ci, @Local BlockEntityRendererProvider.Context context) {
        ModelCopies.replace(ModelCopies.Source.BLOCK_ENTITIES, this.renderers, BlockEntityRenderers.createEntityRenderers(context));
    }
}
