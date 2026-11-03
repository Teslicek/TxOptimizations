package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.TextLayouts;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TextFeatureRenderer.Content.Text.class)
public abstract class TextContentLayoutsMixin implements TextLayouts {

    @Unique
    private @Nullable Font txoptimizations$font;

    @Unique
    private int txoptimizations$generation;

    @Unique
    private Font.@Nullable PreparedText txoptimizations$text;

    @Unique
    private Font.@Nullable PreparedText txoptimizations$outline;

    @Override
    public Font.@Nullable PreparedText txoptimizations$layout(Font font, int generation, boolean outline) {
        if (this.txoptimizations$font != font || this.txoptimizations$generation != generation)
            return null;

        return outline ? this.txoptimizations$outline : this.txoptimizations$text;
    }

    @Override
    public void txoptimizations$setLayout(Font font, int generation, boolean outline, Font.PreparedText layout) {
        if (this.txoptimizations$font != font || this.txoptimizations$generation != generation) {
            this.txoptimizations$font       = font;
            this.txoptimizations$generation = generation;
            this.txoptimizations$text       = null;
            this.txoptimizations$outline    = null;
        }

        if (outline)
            this.txoptimizations$outline = layout;
        else
            this.txoptimizations$text = layout;
    }
}
