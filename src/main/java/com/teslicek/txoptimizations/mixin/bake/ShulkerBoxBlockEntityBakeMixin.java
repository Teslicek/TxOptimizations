package com.teslicek.txoptimizations.mixin.bake;

import com.teslicek.txoptimizations.bake.Baking;
import com.teslicek.txoptimizations.bake.RenderMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShulkerBoxBlockEntity.class)
public abstract class ShulkerBoxBlockEntityBakeMixin {

    @Inject(method = "updateAnimation", at = @At("RETURN"))
    private void txoptimizations$followLid(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        ShulkerBoxBlockEntity box = (ShulkerBoxBlockEntity) (Object) this;

        Baking.requestMode(box, box.getAnimationStatus() == ShulkerBoxBlockEntity.AnimationStatus.CLOSED ? RenderMode.TERRAIN : RenderMode.ENTITY);
    }
}
