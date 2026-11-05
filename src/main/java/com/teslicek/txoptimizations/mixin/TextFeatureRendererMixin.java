package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.teslicek.txoptimizations.TextLayoutCache;
import com.teslicek.txoptimizations.TextLayouts;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TextFeatureRenderer.class)
public abstract class TextFeatureRendererMixin {

    @Redirect(method = "renderText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;"))
    private static Font.PreparedText txoptimizations$reuseTextLayout(Font font, FormattedCharSequence text, float x, float y, int color, boolean dropShadow, boolean includeEmpty, int backgroundColor, @Local(argsOnly = true) TextFeatureRenderer.Content.Text content) {
        if (includeEmpty)
            throw new IllegalStateException("World text layout is not expected to include empty glyphs");

        Font.PreparedText known = ((TextLayouts) (Object) content).txoptimizations$layout(font, TextLayoutCache.generation(), false);

        if (known != null)
            return known;

        Font.PreparedText cached = TextLayoutCache.find(font, text, x, y, color, dropShadow, backgroundColor, false, false);

        if (cached != null)
            return cached;

        return TextLayoutCache.store(font.prepareText(text, x, y, color, dropShadow, false, backgroundColor), font, text, x, y, color, dropShadow, backgroundColor, false, false);
    }

    @Redirect(method = "renderText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;prepare8xTextOutline(Lnet/minecraft/util/FormattedCharSequence;FFI)Lnet/minecraft/client/gui/Font$PreparedText;"))
    private static Font.PreparedText txoptimizations$reuseOutlineLayout(Font font, FormattedCharSequence text, float x, float y, int outlineColor, @Local(argsOnly = true) TextFeatureRenderer.Content.Text content) {
        Font.PreparedText known = ((TextLayouts) (Object) content).txoptimizations$layout(font, TextLayoutCache.generation(), true);

        if (known != null)
            return known;

        Font.PreparedText cached = TextLayoutCache.find(font, text, x, y, outlineColor, false, 0, false, true);

        if (cached != null)
            return cached;

        return TextLayoutCache.store(font.prepare8xTextOutline(text, x, y, outlineColor), font, text, x, y, outlineColor, false, 0, false, true);
    }

    @Redirect(method = "buildGroup", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;set(Lorg/joml/Matrix4fc;)Lorg/joml/Matrix4f;"), require = 1)
    private Matrix4f txoptimizations$copyChangedPose(Matrix4f pose, Matrix4fc source, @Share("copiedPose") LocalRef<Matrix4fc> copied) {
        if (copied.get() == source)
            return pose;

        copied.set(source);

        return pose.set(source);
    }
}
