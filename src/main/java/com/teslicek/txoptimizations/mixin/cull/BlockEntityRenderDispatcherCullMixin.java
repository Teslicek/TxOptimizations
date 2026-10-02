package com.teslicek.txoptimizations.mixin.cull;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.cull.EntityCulling;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherCullMixin {

    @ModifyExpressionValue(method = "tryExtractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;getRenderer(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;"))
    private BlockEntityRenderer<?, ?> txoptimizations$skipCulledBlockEntities(BlockEntityRenderer<?, ?> renderer, @Local(argsOnly = true) BlockEntity blockEntity, @Local(argsOnly = true) boolean isGloballyRendered) {
        if (renderer == null || isGloballyRendered || renderer.shouldRenderOffScreen())
            return renderer;

        return EntityCulling.hidesBlockEntity(blockEntity) ? null : renderer;
    }
}
