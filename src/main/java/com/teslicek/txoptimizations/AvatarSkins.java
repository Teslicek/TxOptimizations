package com.teslicek.txoptimizations;

import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.world.entity.player.PlayerSkin;

public final class AvatarSkins {

    private AvatarSkins() {
    }

    public static PlayerSkin frameSkin(ClientAvatarEntity avatar) {
        AvatarSkinCache cache = (AvatarSkinCache) avatar;
        long            frame = ClientClock.frame();

        if (cache.txoptimizations$getSkinFrame() == frame)
            return cache.txoptimizations$getSkin();

        PlayerSkin skin = avatar.getSkin();
        cache.txoptimizations$setSkin(frame, skin);

        return skin;
    }
}
