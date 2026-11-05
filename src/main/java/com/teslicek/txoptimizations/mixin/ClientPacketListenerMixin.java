package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.teslicek.txoptimizations.ChatSessionCache;
import com.teslicek.txoptimizations.ProfileCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.util.SignatureValidator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

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

    @WrapOperation(method = "initializeChatSession", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/RemoteChatSession$Data;validate(Lcom/mojang/authlib/GameProfile;Lnet/minecraft/util/SignatureValidator;)Lnet/minecraft/network/chat/RemoteChatSession;"), require = 1)
    private RemoteChatSession txoptimizations$validateOnce(RemoteChatSession.Data data, GameProfile profile, SignatureValidator validator, Operation<RemoteChatSession> original) {
        ChatSessionCache.Key key    = ChatSessionCache.key(Minecraft.getInstance().services().servicesKeySet(), profile.id(), data);
        RemoteChatSession    cached = ChatSessionCache.find(key);

        if (cached != null)
            return cached;

        RemoteChatSession session = original.call(data, profile, validator);

        ChatSessionCache.store(key, session);

        return session;
    }
}
