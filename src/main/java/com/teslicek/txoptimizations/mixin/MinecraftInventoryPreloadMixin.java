package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.InventoryClassPreload;
import net.minecraft.client.GameLoadCookie;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftInventoryPreloadMixin {

    @Inject(method = "onGameLoadFinished", at = @At("TAIL"))
    private void txoptimizations$preloadInventoryClasses(GameLoadCookie cookie, CallbackInfo ci) {
        InventoryClassPreload.start();
    }
}
