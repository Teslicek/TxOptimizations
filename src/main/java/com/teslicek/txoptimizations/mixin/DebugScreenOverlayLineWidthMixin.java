package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.TextLayoutCache;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayLineWidthMixin {

    @Redirect(method = "extractLines", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;width(Ljava/lang/String;)I"))
    private int txoptimizations$cachedLineWidth(Font font, String line) {
        return TextLayoutCache.width(font, line);
    }
}
