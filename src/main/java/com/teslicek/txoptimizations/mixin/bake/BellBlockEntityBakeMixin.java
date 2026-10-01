package com.teslicek.txoptimizations.mixin.bake;

import com.teslicek.txoptimizations.bake.Baking;
import com.teslicek.txoptimizations.bake.RenderMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BellBlockEntity.class)
public abstract class BellBlockEntityBakeMixin {

    @Inject(method = "tick", at = @At("RETURN"))
    private static void txoptimizations$followSwing(Level level, BlockPos pos, BlockState state, BellBlockEntity bell, @Coerce Object onResonationEnd, CallbackInfo ci) {
        if (!level.isClientSide())
            return;

        Baking.requestMode(bell, bell.shaking ? RenderMode.ENTITY : RenderMode.TERRAIN);
    }
}
