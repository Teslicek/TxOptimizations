package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.TextLayoutCache;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TextFeatureRenderer.class)
public abstract class TextFeatureRendererLayoutCacheMixin {

    @Redirect(method = "renderText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;"))
    private static Font.PreparedText txoptimizations$reuseTextLayout(Font font, FormattedCharSequence text, float x, float y, int color, boolean dropShadow, boolean includeEmpty, int backgroundColor) {
        if (includeEmpty)
            throw new IllegalStateException("World text layout is not expected to include empty glyphs");

        Font.PreparedText cached = TextLayoutCache.find(font, text, x, y, color, dropShadow, backgroundColor, false);

        if (cached != null)
            return cached;

        return TextLayoutCache.store(font.prepareText(text, x, y, color, dropShadow, false, backgroundColor), font, text, x, y, color, dropShadow, backgroundColor, false);
    }

    @Redirect(method = "renderText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;prepare8xTextOutline(Lnet/minecraft/util/FormattedCharSequence;FFI)Lnet/minecraft/client/gui/Font$PreparedText;"))
    private static Font.PreparedText txoptimizations$reuseOutlineLayout(Font font, FormattedCharSequence text, float x, float y, int outlineColor) {
        Font.PreparedText cached = TextLayoutCache.find(font, text, x, y, outlineColor, false, 0, true);

        if (cached != null)
            return cached;

        return TextLayoutCache.store(font.prepare8xTextOutline(text, x, y, outlineColor), font, text, x, y, outlineColor, false, 0, true);
    }
}
