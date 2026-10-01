package com.teslicek.txoptimizations.mixin.bake;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.teslicek.txoptimizations.bake.BakedModels;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockStateModelSet.class)
public abstract class BlockStateModelSetBakeMixin {

    @ModifyReturnValue(method = "get", at = @At("RETURN"))
    private BlockStateModel txoptimizations$withBlockEntity(BlockStateModel vanilla, BlockState state) {
        return BakedModels.withBlockEntity(state, vanilla);
    }
}
