package com.teslicek.txoptimizations.mixin.bake;

import com.teslicek.txoptimizations.bake.Baking;
import com.teslicek.txoptimizations.bake.RenderMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
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
        float      openness = state.getBlock() instanceof ChestBlock chestBlock && chest.hasLevel() ? chestBlock.combine(state, level, pos, true).apply(ChestBlock.opennessCombiner(chest)).get(PARTIAL_TICK) : chest.getOpenNess(PARTIAL_TICK);
        RenderMode mode     = openness > 0.0F ? RenderMode.ENTITY : RenderMode.TERRAIN;

        Baking.requestMode(chest, mode);

        if (state.getValueOrElse(ChestBlock.TYPE, ChestType.SINGLE) == ChestType.SINGLE)
            return;

        BlockPos    otherPos  = ChestBlock.getConnectedBlockPos(pos, state);
        BlockEntity otherHalf = level.getBlockEntity(otherPos);

        if (otherHalf instanceof ChestBlockEntity && level.getBlockState(otherPos).is(state.getBlock()))
            Baking.requestMode(otherHalf, mode);
    }
}
