package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.BlockEntityLight;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockEntityRenderState.class)
public abstract class BlockEntityRenderStateLightMixin {

    @Redirect(method = "extractBase", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/LightCoordsUtil;getLightCoords(Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/core/BlockPos;)I"))
    private static int txoptimizations$cachedLight(BlockAndLightGetter level, BlockPos pos, BlockEntity blockEntity, BlockEntityRenderState state, @Nullable CrumblingOverlay breakProgress) {
        return BlockEntityLight.lightCoords(level, pos, blockEntity);
    }
}
