package com.teslicek.txoptimizations.mixin.bake;

import com.teslicek.txoptimizations.bake.Baking;
import com.teslicek.txoptimizations.bake.RenderMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityBakeMixin {

    @Unique
    private static final float PARTIAL_TICK = 0.5F;

    @Inject(method = "lidAnimateTick", at = @At("RETURN"))
    private static void txoptimizations$followLid(Level level, BlockPos pos, BlockState state, ChestBlockEntity chest, CallbackInfo ci) {
        ChestBlockEntity otherHalf = txoptimizations$otherHalf(level, pos, state);
        float            openness  = otherHalf == null ? chest.getOpenNess(PARTIAL_TICK) : Math.max(chest.getOpenNess(PARTIAL_TICK), otherHalf.getOpenNess(PARTIAL_TICK));
        RenderMode       mode      = openness > 0.0F ? RenderMode.ENTITY : RenderMode.TERRAIN;

        Baking.requestMode(chest, mode);

        if (otherHalf != null)
            Baking.requestMode(otherHalf, mode);
    }

    @Unique
    private static ChestBlockEntity txoptimizations$otherHalf(Level level, BlockPos pos, BlockState state) {
        ChestType type = state.getValue(ChestBlock.TYPE);

        if (type == ChestType.SINGLE)
            return null;

        BlockPos   otherPos   = ChestBlock.getConnectedBlockPos(pos, state);
        BlockState otherState = level.getBlockState(otherPos);

        if (!otherState.is(state.getBlock()) || otherState.getValue(ChestBlock.TYPE) != type.getOpposite() || otherState.getValue(ChestBlock.FACING) != state.getValue(ChestBlock.FACING))
            return null;

        return level.getBlockEntity(otherPos) instanceof ChestBlockEntity otherChest ? otherChest : null;
    }
}
