package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.NameTags;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobRenderer.class)
public abstract class MobRendererNameMixin {

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Mob;D)Z", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$skipUnnamed(Mob entity, double distanceToCameraSq, CallbackInfoReturnable<Boolean> cir) {
        if (!NameTags.maySeeName(entity))
            cir.setReturnValue(false);
    }
}
