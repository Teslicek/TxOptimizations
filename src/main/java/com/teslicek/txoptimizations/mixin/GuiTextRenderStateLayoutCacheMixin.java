package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.TextLayoutCache;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiTextRenderState.class)
public abstract class GuiTextRenderStateLayoutCacheMixin {

    @Redirect(method = "ensurePrepared", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;"))
    private Font.PreparedText txoptimizations$reuseSharedLayout(Font font, FormattedCharSequence text, float x, float y, int color, boolean dropShadow, boolean includeEmpty, int backgroundColor) {
        if (includeEmpty || !TextLayoutCache.isRepeated(text))
            return font.prepareText(text, x, y, color, dropShadow, includeEmpty, backgroundColor);

        Font.PreparedText cached = TextLayoutCache.find(font, text, x, y, color, dropShadow, backgroundColor, false);

        if (cached != null)
            return cached;

        return TextLayoutCache.store(font.prepareText(text, x, y, color, dropShadow, false, backgroundColor), font, text, x, y, color, dropShadow, backgroundColor, false);
    }
}
