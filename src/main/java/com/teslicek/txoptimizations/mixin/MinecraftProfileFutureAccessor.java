package com.teslicek.txoptimizations.mixin;

import com.mojang.authlib.services.ProfileResult;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftProfileFutureAccessor {

    @Accessor("profileFuture")
    CompletableFuture<ProfileResult> txoptimizations$profileFuture();
}
