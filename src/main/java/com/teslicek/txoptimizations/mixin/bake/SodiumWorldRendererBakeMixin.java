package com.teslicek.txoptimizations.mixin.bake;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.bake.Baking;
import com.teslicek.txoptimizations.bake.EmptyBlockEntities;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SodiumWorldRenderer.class)
public abstract class SodiumWorldRendererBakeMixin {

    @Inject(method = "extractBlockEntity", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$skipEmptyBlockEntities(CallbackInfo ci, @Local(argsOnly = true) BlockEntity blockEntity) {
        if (!EmptyBlockEntities.isEmpty(blockEntity))
            return;

        ci.cancel();
    }

    @ModifyReturnValue(method = "isEntityVisible", at = @At("RETURN"))
    private boolean txoptimizations$hideMeshedEntities(boolean visible, @Local(argsOnly = true) Entity entity) {
        return visible && !Baking.isEntityMeshed(entity);
    }
}
