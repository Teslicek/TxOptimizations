package com.teslicek.txoptimizations.mixin.bake;

import com.teslicek.txoptimizations.bake.Baking;
import com.teslicek.txoptimizations.bake.RenderMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderChestBlockEntity.class)
public abstract class EnderChestBlockEntityBakeMixin {

    @Unique
    private static final float PARTIAL_TICK = 0.5F;

    @Inject(method = "lidAnimateTick", at = @At("RETURN"))
    private static void txoptimizations$followLid(Level level, BlockPos pos, BlockState state, EnderChestBlockEntity chest, CallbackInfo ci) {
        Baking.requestMode(chest, chest.getOpenNess(PARTIAL_TICK) > 0.0F ? RenderMode.ENTITY : RenderMode.TERRAIN);
    }
}
