package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teslicek.txoptimizations.TextLayoutCache;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TextFeatureRenderer.class)
public abstract class TextFeatureRendererLayoutCacheMixin {

    @WrapOperation(method = "renderText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;"))
    private static Font.PreparedText txoptimizations$reuseTextLayout(Font font, FormattedCharSequence text, float x, float y, int color, boolean dropShadow, boolean includeEmpty, int backgroundColor, Operation<Font.PreparedText> original) {
        if (includeEmpty)
            throw new IllegalStateException("World text layout is not expected to include empty glyphs");

        return TextLayoutCache.get(font, text, x, y, color, dropShadow, backgroundColor, false, () -> original.call(font, text, x, y, color, dropShadow, includeEmpty, backgroundColor));
    }

    @WrapOperation(method = "renderText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;prepare8xTextOutline(Lnet/minecraft/util/FormattedCharSequence;FFI)Lnet/minecraft/client/gui/Font$PreparedText;"))
    private static Font.PreparedText txoptimizations$reuseOutlineLayout(Font font, FormattedCharSequence text, float x, float y, int outlineColor, Operation<Font.PreparedText> original) {
        return TextLayoutCache.get(font, text, x, y, outlineColor, false, 0, true, () -> original.call(font, text, x, y, outlineColor));
    }
}
