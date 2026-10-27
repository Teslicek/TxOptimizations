package com.teslicek.txoptimizations.mixin.bake;

import com.teslicek.txoptimizations.TextLayoutCache;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractSignRenderer.class)
public abstract class SignTextWidthMixin {

    @Redirect(method = "submitSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;width(Lnet/minecraft/util/FormattedCharSequence;)I"))
    private int txoptimizations$cachedLineWidth(Font font, FormattedCharSequence line) {
        return TextLayoutCache.width(font, line);
    }
}
