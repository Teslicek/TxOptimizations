package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BrushableBlockRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BrushableBlockExtractSkipMixin {

    @ModifyExpressionValue(method = "tryExtractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;getRenderer(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;"))
    private BlockEntityRenderer<?, ?> txoptimizations$skipUndustedBrushable(BlockEntityRenderer<?, ?> renderer, @Local(argsOnly = true) BlockEntity blockEntity) {
        if (!(renderer instanceof BrushableBlockRenderer))
            return renderer;

        BlockState state = blockEntity.getBlockState();

        if (state.hasProperty(BlockStateProperties.DUSTED) && state.getValue(BlockStateProperties.DUSTED) == 0)
            return null;

        return renderer;
    }
}
