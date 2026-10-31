package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.NameTags;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererNameMixin {

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$skipUnnamed(Avatar entity, double distanceToCameraSq, CallbackInfoReturnable<Boolean> cir) {
        if (!NameTags.maySeeName(entity))
            cir.setReturnValue(false);
    }
}
