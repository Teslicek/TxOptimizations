package com.teslicek.txoptimizations.mixin.bake;

import com.teslicek.txoptimizations.bake.Bakeable;
import com.teslicek.txoptimizations.bake.Baking;
import com.teslicek.txoptimizations.bake.RenderMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkullBlockEntity.class)
public abstract class SkullBlockEntityBakeMixin {

    @Inject(method = "animation", at = @At("TAIL"))
    private static void txoptimizations$followAnimation(Level level, BlockPos pos, BlockState state, SkullBlockEntity skull, CallbackInfo ci) {
        Baking.requestMode(skull, skull.isAnimating || skull.getOwnerProfile() != null ? RenderMode.ENTITY : RenderMode.TERRAIN);
    }

    @Inject(method = {"loadAdditional", "applyImplicitComponents"}, at = @At("RETURN"))
    private void txoptimizations$keepProfileSkullsAsEntities(CallbackInfo ci) {
        if (((SkullBlockEntity) (Object) this).getOwnerProfile() == null)
            return;

        ((Bakeable) this).txoptimizations$setRenderMode(RenderMode.ENTITY);
    }
}
