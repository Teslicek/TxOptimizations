package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
public class HideLevelLoadingScreenMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$skipRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (Minecraft.getInstance().level == null)
            return;

        ci.cancel();
    }

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$skipBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (Minecraft.getInstance().level == null)
            return;

        ci.cancel();
    }
}
