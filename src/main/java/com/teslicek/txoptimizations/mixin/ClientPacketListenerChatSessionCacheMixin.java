package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import com.teslicek.txoptimizations.ChatSessionCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.util.SignatureValidator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerChatSessionCacheMixin {

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
