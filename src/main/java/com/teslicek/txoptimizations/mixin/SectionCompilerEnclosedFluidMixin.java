package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SectionCompiler.class)
public abstract class SectionCompilerEnclosedFluidMixin {

    @Unique
    private static final Direction[] NEIGHBORS = {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    @WrapWithCondition(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/FluidRenderer;tesselate(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/client/renderer/block/FluidRenderer$Output;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)V"))
    private boolean txoptimizations$skipEnclosedFluid(FluidRenderer renderer, BlockAndTintGetter level, BlockPos pos, FluidRenderer.Output output, BlockState blockState, FluidState fluidState, @Share("fluidNeighbor") LocalRef<BlockPos.MutableBlockPos> neighborRef) {
        Fluid fluid = fluidState.getType();

        if (FluidRenderingRegistry.getOverride(fluid) != null)
            return true;

        BlockPos.MutableBlockPos neighbor = neighborRef.get();

        if (neighbor == null) {
            neighbor = new BlockPos.MutableBlockPos();
            neighborRef.set(neighbor);
        }

        for (Direction direction : NEIGHBORS) {
            if (!level.getBlockState(neighbor.setWithOffset(pos, direction)).getFluidState().getType().isSame(fluid))
                return true;
        }

        return false;
    }
}
