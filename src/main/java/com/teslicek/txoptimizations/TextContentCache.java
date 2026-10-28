package com.teslicek.txoptimizations;

import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.util.FormattedCharSequence;

public final class TextContentCache {

    private static final int                                SLOTS = 4096;
    private static final TextFeatureRenderer.Content.Text[] TEXTS = new TextFeatureRenderer.Content.Text[SLOTS];

    private TextContentCache() {
    }

    public static TextFeatureRenderer.Content.Text text(float x, float y, FormattedCharSequence string, boolean dropShadow, int color, int backgroundColor, int outlineColor) {
        int                              slot = System.identityHashCode(string) & SLOTS - 1;
        TextFeatureRenderer.Content.Text last = TEXTS[slot];

        if (last != null && last.string() == string && Float.floatToRawIntBits(last.x()) == Float.floatToRawIntBits(x) && Float.floatToRawIntBits(last.y()) == Float.floatToRawIntBits(y) && last.dropShadow() == dropShadow && last.color() == color && last.backgroundColor() == backgroundColor && last.outlineColor() == outlineColor)
            return last;

        TextFeatureRenderer.Content.Text text = new TextFeatureRenderer.Content.Text(x, y, string, dropShadow, color, backgroundColor, outlineColor);

        TEXTS[slot] = text;

        return text;
    }
}
