package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.AvatarSkinCache;
import com.teslicek.txoptimizations.ClientClock;
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
        AvatarSkinCache cache = (AvatarSkinCache) avatar;
        long            frame = ClientClock.frame();

        if (cache.txoptimizations$getSkinFrame() == frame)
            return cache.txoptimizations$getSkin();

        PlayerSkin skin = avatar.getSkin();
        cache.txoptimizations$setSkin(frame, skin);

        return skin;
    }
}
