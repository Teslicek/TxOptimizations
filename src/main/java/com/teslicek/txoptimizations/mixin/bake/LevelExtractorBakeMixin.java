package com.teslicek.txoptimizations.mixin.bake;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.bake.Baking;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorBakeMixin {

    @ModifyReturnValue(method = "isEntityVisible", at = @At("RETURN"))
    private boolean txoptimizations$hideMeshedEntities(boolean visible, @Local(argsOnly = true) Entity entity) {
        return visible && !Baking.isEntityMeshed(entity);
    }
}
