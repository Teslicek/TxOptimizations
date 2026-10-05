package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.BlockEntityDistance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDistanceMixin {

    @Redirect(method = "tryExtractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;shouldRender(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/phys/Vec3;)Z"))
    private boolean txoptimizations$distanceWithoutVector(BlockEntityRenderer<?, ?> renderer, BlockEntity blockEntity, Vec3 cameraPosition) {
        return BlockEntityDistance.shouldRender(renderer, blockEntity, cameraPosition);
    }
}
