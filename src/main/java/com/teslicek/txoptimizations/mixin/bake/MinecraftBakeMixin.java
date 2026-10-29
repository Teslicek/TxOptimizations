package com.teslicek.txoptimizations.mixin.bake;

import com.teslicek.txoptimizations.bake.ChunkTasks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftBakeMixin {

    @Inject(method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V", at = @At("HEAD"))
    private void txoptimizations$dropPendingSectionTasks(ClientLevel level, boolean stopSound, CallbackInfo ci) {
        ChunkTasks.clear();
    }
}
