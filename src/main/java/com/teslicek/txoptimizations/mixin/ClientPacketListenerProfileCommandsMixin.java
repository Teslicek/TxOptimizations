package com.teslicek.txoptimizations.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.teslicek.txoptimizations.ProfileCommands;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerProfileCommandsMixin {

    @Shadow
    private CommandDispatcher<ClientSuggestionProvider> commands;

    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$runProfileCommand(String command, CallbackInfo ci) {
        if (ProfileCommands.execute((ClientPacketListener) (Object) this, command))
            ci.cancel();
    }

    @Inject(method = "sendUnattendedCommand", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$runUnattendedProfileCommand(String command, Screen screenAfterCommand, CallbackInfo ci) {
        if (ProfileCommands.execute((ClientPacketListener) (Object) this, command))
            ci.cancel();
    }

    @Inject(method = "handleCommands", at = @At("RETURN"))
    private void txoptimizations$suggestProfileCommands(CallbackInfo ci) {
        ProfileCommands.addSuggestions(this.commands);
    }
}
