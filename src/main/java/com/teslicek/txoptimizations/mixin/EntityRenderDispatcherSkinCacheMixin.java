package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.AvatarSkins;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherSkinCacheMixin {

    @Redirect(method = "getAvatarRenderer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/ClientAvatarEntity;getSkin()Lnet/minecraft/world/entity/player/PlayerSkin;"))
    private PlayerSkin txoptimizations$reuseFrameSkin(ClientAvatarEntity avatar) {
        return AvatarSkins.frameSkin(avatar);
    }
}
