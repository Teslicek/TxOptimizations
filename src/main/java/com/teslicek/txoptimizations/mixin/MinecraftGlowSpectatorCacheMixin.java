package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teslicek.txoptimizations.ClientClock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class MinecraftGlowSpectatorCacheMixin {

    @Unique
    private long txoptimizations$spectatorFrame = -1L;

    @Unique
    private LocalPlayer txoptimizations$spectatorPlayer;

    @Unique
    private boolean txoptimizations$spectator;

    @WrapOperation(method = "shouldEntityAppearGlowing", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"))
    private boolean txoptimizations$reuseFrameSpectator(LocalPlayer player, Operation<Boolean> original) {
        long frame = ClientClock.frame();

        if (frame == this.txoptimizations$spectatorFrame && player == this.txoptimizations$spectatorPlayer)
            return this.txoptimizations$spectator;

        this.txoptimizations$spectator       = original.call(player);
        this.txoptimizations$spectatorFrame  = frame;
        this.txoptimizations$spectatorPlayer = player;

        return this.txoptimizations$spectator;
    }
}
