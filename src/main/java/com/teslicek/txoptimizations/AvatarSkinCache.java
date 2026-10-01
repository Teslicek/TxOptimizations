package com.teslicek.txoptimizations;

import net.minecraft.world.entity.player.PlayerSkin;

public interface AvatarSkinCache {

    long txoptimizations$getSkinFrame();

    PlayerSkin txoptimizations$getSkin();

    void txoptimizations$setSkin(long frame, PlayerSkin skin);
}
