package com.teslicek.txoptimizations.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClipContext.class)
public abstract class ClipContextAirMixin {

    @Shadow
    @Final
    private ClipContext.Block block;

    @Shadow
    @Final
    private CollisionContext collisionContext;

    @Overwrite
    public VoxelShape getBlockShape(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.isAir())
            return Shapes.empty();

        return this.block.get(state, level, pos, this.collisionContext);
    }
}
