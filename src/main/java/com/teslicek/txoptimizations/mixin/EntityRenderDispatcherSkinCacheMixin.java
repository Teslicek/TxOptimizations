package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teslicek.txoptimizations.AvatarSkinCache;
import com.teslicek.txoptimizations.ClientClock;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherSkinCacheMixin {

    @WrapOperation(method = "getAvatarRenderer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/ClientAvatarEntity;getSkin()Lnet/minecraft/world/entity/player/PlayerSkin;"))
    private PlayerSkin txoptimizations$reuseFrameSkin(ClientAvatarEntity avatar, Operation<PlayerSkin> original) {
        AvatarSkinCache cache = (AvatarSkinCache) avatar;
        long            frame = ClientClock.frame();

        if (cache.txoptimizations$getSkinFrame() == frame)
            return cache.txoptimizations$getSkin();

        PlayerSkin skin = original.call(avatar);
        cache.txoptimizations$setSkin(frame, skin);

        return skin;
    }
}
