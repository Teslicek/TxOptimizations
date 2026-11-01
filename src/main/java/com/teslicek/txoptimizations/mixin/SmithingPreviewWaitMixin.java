package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.RenderWorker;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmithingScreen.class)
public abstract class SmithingPreviewWaitMixin {

    @Inject(method = "updateArmorStandPreview", at = @At("HEAD"), require = 1)
    private void txoptimizations$waitBeforeUpdatingPreview(CallbackInfo ci) {
        RenderWorker.awaitFromMain();
    }
}
