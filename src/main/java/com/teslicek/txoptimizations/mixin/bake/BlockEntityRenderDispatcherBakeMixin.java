package com.teslicek.txoptimizations.mixin.bake;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.bake.BakeableBlockEntity;
import com.teslicek.txoptimizations.bake.Baking;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherBakeMixin {

    @ModifyExpressionValue(method = "tryExtractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;getRenderer(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;"))
    private BlockEntityRenderer<?, ?> txoptimizations$skipMeshedBlockEntities(BlockEntityRenderer<?, ?> renderer, @Local(argsOnly = true) BlockEntity blockEntity) {
        if (renderer == null || !((BakeableBlockEntity) blockEntity).txoptimizations$isBakeSupported() || blockEntity.getType() == BlockEntityTypes.BANNER)
            return renderer;

        if (blockEntity.getType() == BlockEntityTypes.COPPER_GOLEM_STATUE)
            return Baking.isBaked(blockEntity) ? null : renderer;

        return Baking.shouldRenderEntity(blockEntity) ? renderer : null;
    }
}
