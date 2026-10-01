package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.AvatarSkinCache;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Avatar.class)
public abstract class AvatarSkinCacheMixin implements AvatarSkinCache {

    @Unique
    private long txoptimizations$skinFrame = -1L;

    @Unique
    private PlayerSkin txoptimizations$skin;

    @Override
    public long txoptimizations$getSkinFrame() {
        return this.txoptimizations$skinFrame;
    }

    @Override
    public PlayerSkin txoptimizations$getSkin() {
        return this.txoptimizations$skin;
    }

    @Override
    public void txoptimizations$setSkin(long frame, PlayerSkin skin) {
        this.txoptimizations$skinFrame = frame;
        this.txoptimizations$skin      = skin;
    }
}
