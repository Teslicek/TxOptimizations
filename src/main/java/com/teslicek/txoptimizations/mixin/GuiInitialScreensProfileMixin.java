package com.teslicek.txoptimizations.mixin;

import com.mojang.authlib.services.ProfileResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Gui.class)
public abstract class GuiInitialScreensProfileMixin {

    @Redirect(method = "addInitialScreens", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getProfileResult()Lcom/mojang/authlib/services/ProfileResult;"))
    private @Nullable ProfileResult txoptimizations$profileIfFetched(Minecraft minecraft) {
        return ((MinecraftProfileFutureAccessor) minecraft).txoptimizations$profileFuture().getNow(null);
    }
}
